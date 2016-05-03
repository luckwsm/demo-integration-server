package com.lyric.controllers;

import com.google.common.io.Resources;
import com.lyric.SecurityService;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import org.apache.commons.lang3.StringUtils;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;

import java.io.IOException;
import java.net.URL;

/**
 * Created by amadden on 1/29/16.
 */
public class DemoBaseController {
    Logger logger = LoggerFactory.getLogger(DemoBaseController.class.getName());
    private final SecurityService securityService;

    public DemoBaseController(SecurityService securityService) {
        this.securityService = securityService;
    }

    protected HttpClientRequest getHttpClientRequest(HttpServerRequest req, String uri, Vertx vertx) {
        //String vendorId = getParam(req, "vendorId", System.getenv("DEFAULT_VENDOR_ID"));

        String host = System.getenv("DEFAULT_INTEGRATION_SERVICES_HOST") != null ? System.getenv("DEFAULT_INTEGRATION_SERVICES_HOST") : "demo-dev.lyricfinancial.com";

        URL certificate = Resources.getResource("certificate.pfx");
        PfxOptions options = new PfxOptions();
        PemTrustOptions pemOptions = new PemTrustOptions();
        try {
            options
                    .setPassword("lyric_changeme")
                    .setValue(Buffer.buffer(Resources.toByteArray(certificate)));


            URL intermidiate = Resources.getResource("intermediate.pem");
            logger.info("*******************INTERMEDIATE " + intermidiate);
            URL root = Resources.getResource("root.pem");
            logger.info("*******************ROOT " + root);
            Buffer intermediateBuffer = Buffer.buffer(Resources.toByteArray(intermidiate));
            logger.info("*******************INTERMEDIATE BUFFER " + intermediateBuffer);
            Buffer rootBuffer = Buffer.buffer(Resources.toByteArray(root));
            logger.info("*******************ROOT BUFFER " + rootBuffer);
            pemOptions = new PemTrustOptions()
                    .addCertValue(intermediateBuffer)
                    .addCertValue(rootBuffer);

        } catch (IOException e) {
            e.printStackTrace();
        }


        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions(new JsonObject().put("defaultPort", 443).put("defaultHost", host))
                .setSsl(true)
                .setPemTrustOptions(pemOptions)
                .setPfxKeyCertOptions(options)
        );

        return httpClient.post(uri, cRes -> {
            logger.info("Proxying response: " + cRes.statusCode());
            req.response().setStatusCode(cRes.statusCode());
            req.response().headers().setAll(cRes.headers());

            cRes.bodyHandler(data -> {
                logger.debug("Proxying response body:" + data);
                req.response().end(data);
            });
        });
    }

    protected String getUri(String contentType, HttpServerRequest req) {

        String uri = "/v1/clients";

        if(contentType.substring(0, 9).equals("multipart")){
            uri = "/v1/clients.form";
        }
        return uri;
    }

    protected void setHeaders(HttpClientRequest cReq, HttpServerRequest req) {
        /* 3 headers need to be set in order to call the Registration API.  vendorId, content-type
        and authorization.  vendorId and the username and password to create the credentials will be
        provided to you.  The content-type will get copied from the server request.
        */
        req.headers().remove(HttpHeaders.HOST);
        cReq.headers().setAll(req.headers());

        cReq.putHeader("content-type", "application/jose");

        setAuthorizationHeaders(cReq, req);
    }

    protected void setAuthorizationHeaders(HttpClientRequest cReq, HttpServerRequest req) {
        String vendorId = getParam(req, "vendorId", System.getenv("DEFAULT_VENDOR_ID"));
        cReq.putHeader("vendorId", vendorId);

        String authToken = getParam(req, "authToken", System.getenv("DEFAULT_AUTH_TOKEN"));
        cReq.putHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authToken);
    }

    protected String getParam(HttpServerRequest request, String paramName, String defaultValue) {
        String param = request.getParam(paramName);
        if (StringUtils.isBlank(param)) {
            return defaultValue;
        }
        return param;
    }

    protected boolean shouldLoadRoyaltyEarningsCsv(JsonObject options) {
        return (options.getString("royaltyEarningsContentType").equals("text/csv") || options.getString("royaltyEarningsContentType").equals("application/zip") ) && !options.getString("filename").equals("");
    }

    protected String signAndEncrypt(byte[] payload, String cty) throws JoseException {
        JsonWebSignature jws = securityService.createSignature(payload);
        return securityService.encryptPayload(jws, payload, cty);
    }

    protected void thowSignEncryptError(HttpServerRequest req) {
        req.response().setStatusMessage("Could not sign and encrypt payload.");
        req.response().setStatusCode(500).end();
    }

}
