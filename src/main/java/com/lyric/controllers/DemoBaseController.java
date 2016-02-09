package com.lyric.controllers;

import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;

/**
 * Created by amadden on 1/29/16.
 */
public class DemoBaseController {
    Logger logger = LoggerFactory.getLogger(DemoBaseController.class.getName());

    protected HttpClientRequest getHttpClientRequest(HttpServerRequest req, String uri, Vertx vertx) {
        String host = System.getenv("DEFAULT_INTEGRATION_SERVICES_HOST") != null ? System.getenv("DEFAULT_INTEGRATION_SERVICES_HOST") : "demoservices.lyricfinancial.com";
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions(new JsonObject().put("defaultPort", 443).put("defaultHost", host)).setSsl(true));

        return httpClient.post(uri, cRes -> {
            logger.info("Proxying response: " + cRes.statusCode());
            req.response().setStatusCode(cRes.statusCode());
            req.response().headers().setAll(cRes.headers());
            req.response().setChunked(true);
            cRes.bodyHandler(data -> {
                logger.debug("Proxying response body:" + data);
                req.response().write(data);
                req.response().end();
            });
            req.response().end();
        });
    }

    protected String getUri(String contentType) {
        String uri = "/vendorAPI/v1/clients.json";

        if(contentType.substring(0, 9).equals("multipart")){
            uri = "/vendorAPI/v1/clients.form";
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

        setAuthorizationHeaders(cReq, req);
    }

    protected void setAuthorizationHeaders(HttpClientRequest cReq, HttpServerRequest req) {
        String vendorId = getParam(req, "vendorId", System.getenv("DEFAULT_VENDOR_ID"));
        cReq.putHeader("vendorId", vendorId);

        /* Username and password are used to generate the authorization header.  These values need to
        be base64 encoded to create the new authorization token.
         */
        String authToken = null;
        try {
            String username = getParam(req, "username", System.getenv("DEFAULT_USERNAME"));
            String password = getParam(req, "password", System.getenv("DEFAULT_PASSWORD"));
            authToken = this.createCredentials(username, password);
        } catch (UnsupportedEncodingException e) {
            logger.error("Could not create client credentials", e.getCause());
        }
        cReq.putHeader(HttpHeaders.AUTHORIZATION, "Basic " + authToken);
    }

    private String createCredentials(String username, String password) throws UnsupportedEncodingException {
        return Base64.encodeBase64String(String.format("%s:%s", username, password).getBytes("UTF-8"));
    }

    protected String getParam(HttpServerRequest request, String paramName, String defaultValue) {
        String param = request.getParam(paramName);
        if (StringUtils.isBlank(param)) {
            return defaultValue;
        }
        return param;
    }

    protected boolean shouldLoadRoyaltyEarningsCsv(JsonObject options) {
        return options.getString("royaltyEarningsContentType").equals("text/csv") && !options.getString("filename").equals("");
    }

}
