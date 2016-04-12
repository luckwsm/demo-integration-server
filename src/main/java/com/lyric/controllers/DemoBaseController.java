package com.lyric.controllers;

import com.lyric.SecurityService;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;

import java.io.UnsupportedEncodingException;

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
        String host = System.getenv("DEFAULT_INTEGRATION_SERVICES_HOST") != null ? System.getenv("DEFAULT_INTEGRATION_SERVICES_HOST") : "vendor-api-dev.lyricfinancial.com";
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions(new JsonObject().put("defaultPort", 80).put("defaultHost", host)).setSsl(false));

        return httpClient.post(uri, cRes -> {
            logger.info("Proxying response: " + cRes.statusCode());
            req.response().setStatusCode(cRes.statusCode());
            req.response().headers().setAll(cRes.headers());
//            req.response().setChunked(true);
            cRes.bodyHandler(data -> {
                logger.debug("Proxying response body:" + data);
                req.response().end(data);
            });
        });
    }

    protected String getUri(String contentType, HttpServerRequest req) {
        String vendorId = getParam(req, "vendorId", System.getenv("DEFAULT_VENDOR_ID"));

        String uri = String.format("/%s/vendorApi/v1/clients", vendorId);

        if(contentType.substring(0, 9).equals("multipart")){
            uri = String.format("/%s/vendorApi/v1/clients.form", vendorId);
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
