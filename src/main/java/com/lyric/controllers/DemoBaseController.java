package com.lyric.controllers;

import com.google.common.io.Resources;
import com.lyric.ClientRepository;
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
    protected static String BOUNDARY = "----LyricBoundaryAL0lfjW6DJtKiwkd";

    public DemoBaseController(SecurityService securityService) {
        this.securityService = securityService;
    }

    protected HttpClientRequest getHttpClientRequest(HttpServerRequest req, String uri, Vertx vertx) {

        HttpClient httpClient = getHttpClient(req, vertx);

        return httpClient.post(uri, cRes -> {
            logger.info("Proxying response: " + cRes.statusCode());
            req.response().setStatusCode(cRes.statusCode());
            req.response().headers().setAll(cRes.headers());
            req.response().setChunked(true);

            cRes.bodyHandler(data -> {
                logger.debug("Proxying response body:" + data);
                req.response().end(data);
            });
        });
    }

    private HttpClient getHttpClient(HttpServerRequest request, Vertx vertx) {
        String host = System.getenv("DEFAULT_INTEGRATION_SERVICES_HOST") != null ? System.getenv("DEFAULT_INTEGRATION_SERVICES_HOST") : "demo-dev.lyricfinancial.com";
        boolean isSslOn = Boolean.parseBoolean(getParam(request, "ssl", System.getenv("DEFAULT_SSL_FLAG")));
        int defaultPort = isSslOn ? 443 : 80;

        HttpClientOptions options = new HttpClientOptions(new JsonObject().put("defaultPort", defaultPort).put("defaultHost", host)).setSsl(isSslOn);

        if(isSslOn){
            PfxOptions pfkKeyCertOptions = new PfxOptions();
            PemTrustOptions pemOptions = new PemTrustOptions();

            Buffer certificate = getCert("certificate.pfx");
            Buffer intermediateCertificate = getCert("intermediate.pem");
            Buffer rootCertificate = getCert("root.pem");

            pfkKeyCertOptions
                    .setPassword("lyric_changeme")
                    .setValue(certificate);

            pemOptions
                    .addCertValue(intermediateCertificate)
                    .addCertValue(rootCertificate)
            ;

            options
                    .setPemTrustOptions(pemOptions)
                    .setPfxKeyCertOptions(pfkKeyCertOptions);
        }

        return vertx.createHttpClient(options);
    }

    private Buffer getCert(String fileName){
        try {
            URL file = Resources.getResource(fileName);
            return Buffer.buffer(Resources.toByteArray(file));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    protected String getUri(String contentType) {

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

        final boolean useJose = Boolean.parseBoolean(getParam(req, "jose", System.getenv("DEFAULT_JOSE_FLAG")));
        if(useJose){
            cReq.putHeader("content-type", "application/jose");
        }

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

    protected String signAndEncrypt(byte[] payload, String cty) throws JoseException {
        JsonWebSignature jws = securityService.createSignature(payload);
        return securityService.encryptPayload(jws, payload, cty);
    }

    protected void thowSignEncryptError(HttpServerRequest req) {
        req.response().setStatusMessage("Could not sign and encrypt payload.");
        req.response().setStatusCode(500).end();
    }

    protected Buffer processMultipart(HttpServerRequest req, JsonObject options, JsonObject client, HttpClientRequest cReq) {


        setHeaders(cReq, req);
        String contentType = String.format("multipart/form-data;boundary=%s", BOUNDARY);
        cReq.putHeader("content-type", contentType);

        Buffer body = generateMultipart(req, client, options);


        cReq.setChunked(true);

        return body;
    }

    private Buffer generateMultipart(HttpServerRequest req, JsonObject client, JsonObject options) {
        Buffer body = Buffer.buffer();

        JsonObject fileData = new JsonObject();
        try {
            ClientRepository.getRoyaltyEarnings(fileData, options, client);
        } catch (IOException e) {
            logger.error(String.format("Error getting csv data: %s", e.getMessage()));
        }
        if(fileData.getBinary("data") != null){
            String contentDisposition = "Content-Disposition: form-data; name=\"DistributionGrouping\"; filename=\"" + fileData.getString("filename") + "\"\r\n";
            addDataToBuffer(req, body, contentDisposition, fileData.getBinary("data"), fileData.getString("contentType"));
        }

        String contentDisposition = "Content-Disposition: form-data; name=\"UserProfile\"\r\n";
        addDataToBuffer(req, body, contentDisposition, client.toString().getBytes(), "application/json");
        body.appendString("--" + BOUNDARY + "--\r\n");

        return body;
    }

    private void addDataToBuffer(HttpServerRequest req, Buffer buffer, String contentDisposition, byte[] content, String contentType) {
        final boolean useJose = Boolean.parseBoolean(getParam(req, "jose", System.getenv("DEFAULT_JOSE_FLAG")));
        String encryptedData = null;
        if(useJose) {
            try {
                encryptedData = signAndEncrypt(content, contentType);
            } catch (JoseException e) {
                thowSignEncryptError(req);
            }
            contentType = "application/jose";
        }

        buffer.appendString("--" + BOUNDARY + "\r\n");
        buffer.appendString(contentDisposition);
        buffer.appendString("Content-Type: " + contentType + "\r\n");
        buffer.appendString("\r\n");
        buffer.appendString(encryptedData);
        buffer.appendString("\r\n");
    }

}
