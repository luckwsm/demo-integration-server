package com.lyric.controllers;

import com.google.common.io.Resources;
import com.lyric.ClientRepository;
import com.lyric.SecurityService;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import org.apache.commons.lang3.StringUtils;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by amadden on 1/29/16.
 */
public class DemoBaseController {
    Logger logger = LoggerFactory.getLogger(DemoBaseController.class.getName());
    protected final SecurityService securityService;
    protected static String BOUNDARY = "----LyricBoundaryAL0lfjW6DJtKiwkd";

    public DemoBaseController(SecurityService securityService) {
        this.securityService = securityService;
    }

    protected HttpClientRequest getHttpClientRequest(HttpServerRequest req, String uri, Vertx vertx, String vendorId) {

        HttpClient httpClient = getHttpClient(req, vertx, vendorId);

        return httpClient.post(uri, cRes -> {
            logger.info("Proxying response: " + cRes.statusCode());

            req.response().setStatusCode(cRes.statusCode());
            req.response().headers().setAll(cRes.headers());
            req.response().setChunked(true);

            cRes.bodyHandler(data -> {
                logger.info(data);
                if(cRes.statusCode() != 201 && cRes.statusCode() != 202){
                    req.response().end(data.toString());
                }

                JsonWebEncryption jwe = null;
                try {
                    jwe = securityService.decryptPayload(data.toString(), vendorId);
                } catch (JoseException e) {
                    e.printStackTrace();
                }

                JsonObject response = null;
                try {
                    response = new JsonObject(jwe.getPlaintextString());
                    logger.info("Proxying response body:" + response);
                } catch (JoseException e) {
                    e.printStackTrace();
                }

                req.response().end(response.toString());
            });
        }).setChunked(true);
    }

    protected HttpClient getHttpClient(HttpServerRequest request, Vertx vertx, String vendorId) {
        String host = "";
        final String env = System.getenv("API_ENV");

        if(vendorId != null){
            host = String.format("%s-%s.lyricfinancial.com", vendorId, env);
        }
        else{
            host = System.getenv("DEFAULT_INTEGRATION_SERVICES_HOST") != null ? System.getenv("DEFAULT_INTEGRATION_SERVICES_HOST") : "demo-dev.lyricfinancial.com";
        }


        boolean isSslOn = Boolean.parseBoolean(getParam(request, "ssl", System.getenv("DEFAULT_SSL_FLAG")));
        int defaultPort = isSslOn ? 443 : 80;

        HttpClientOptions options = new HttpClientOptions(new JsonObject().put("defaultPort", defaultPort).put("defaultHost", host)).setSsl(isSslOn);
        options.setLogActivity(true);
        if(isSslOn){
            PfxOptions pfkKeyCertOptions = new PfxOptions();
            PemTrustOptions pemOptions = new PemTrustOptions();

            String fileLocation = env;

            if(vendorId != null){
                fileLocation += "/" + vendorId;
            }

            Buffer certificate = getCert(String.format("%s/certificate.pfx", fileLocation));
            Buffer intermediateCertificate = getCert(String.format("%s/intermediate.pem", fileLocation));
            Buffer rootCertificate = getCert(String.format("%s/root.pem", fileLocation));

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

    protected void setHeaders(HttpClientRequest cReq, HttpServerRequest req, String vendorId) {
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

        setAuthorizationHeaders(cReq, req, vendorId);
    }

    protected void setAuthorizationHeaders(HttpClientRequest cReq, HttpServerRequest req, String vendorId) {

        cReq.putHeader("vendor-id", vendorId);

        final String authTokenName = String.format("%s_%s_AUTH_TOKEN", vendorId, System.getenv("API_ENV")).toUpperCase();
        logger.info("AUTH TOKEN NAME: " + authTokenName);
        String authToken = System.getenv(authTokenName);

        if(authToken == null){
            authToken = getParam(req, "authToken", System.getenv("DEFAULT_AUTH_TOKEN"));
        }

        cReq.putHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authToken);
    }

    protected String getParam(HttpServerRequest request, String paramName, String defaultValue) {
        String param = request.getParam(paramName);
        if (StringUtils.isBlank(param)) {
            return defaultValue;
        }
        return param;
    }

    protected JsonObject getClientData(String clientId, JsonObject data, String vendorId) {
        JsonObject client = data.getJsonObject("client", null);

        if(client == null){
            JsonObject clientOptions = data.getJsonObject("clientOptions", null);

            /* Look up client data from your system */
            client = ClientRepository.findClient(clientId, false, vendorId, clientOptions);
        }
        return client;
    }

    protected String signAndEncrypt(byte[] payload, String cty, JsonArray additionalJweHeaders, String vendorId) throws JoseException {
        JsonWebSignature jws = securityService.createSignature(payload, vendorId);
        return securityService.encryptPayload(jws, payload, cty, additionalJweHeaders, vendorId);
    }

    protected void thowSignEncryptError(HttpServerRequest req) {
        req.response().setStatusMessage("Could not sign and encrypt payload.");
        req.response().setStatusCode(500).end();
    }

    protected JsonObject getRegistrationRequestMultipartDetails(){
        return new JsonObject()
                .put("contentDisposition", "Content-Disposition: form-data; name=\"RegistrationRequest\"\n")
                .put("contentType", "application/json");
    }

    protected JsonObject getFileMultipartDetails(JsonObject fileData){
        return new JsonObject()
                .put("contentDisposition", "Content-Disposition: form-data; name=\"FinancialRecordGroupingFileSet\"; filename=\"" + fileData.getString("filename") + "\"\n")
                .put("contentType", fileData.getString("contentType") + "; lyric-fileset-file-type=" + fileData.getString("filesetFileType", "songSummary") + "; lyric-csv-schema=" + fileData.getString("csvSchema", "TunecoreDistributionSample"));
    }



    protected void addDataToBuffer(Buffer buffer, byte[] content, JsonObject multipartDetails, boolean useJose, JsonArray additionalHeaders, String vendorId) throws JoseException {

        String data = new String(content);
        if(useJose) {
            data = signAndEncrypt(content, multipartDetails.getString("contentType"), additionalHeaders, vendorId);
            multipartDetails.put("contentType", "application/jose");
        }

        buffer.appendString("--" + BOUNDARY + "\n");
        buffer.appendString(multipartDetails.getString("contentDisposition"));
        buffer.appendString("Content-Type: " + multipartDetails.getString("contentType") + "\n");
        buffer.appendString("\n");
        buffer.appendString(data);
        buffer.appendString("\n");
    }

}
