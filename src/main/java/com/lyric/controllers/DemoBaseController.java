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
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;

import java.io.IOException;
import java.net.URL;

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

    protected HttpClientRequest getHttpClientRequest(HttpServerRequest req, String uri, Vertx vertx) {

        HttpClient httpClient = getHttpClient(req, vertx);

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
                    jwe = securityService.decryptPayload(data.toString());
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

    protected HttpClient getHttpClient(HttpServerRequest request, Vertx vertx) {
        String host = System.getenv("DEFAULT_INTEGRATION_SERVICES_HOST") != null ? System.getenv("DEFAULT_INTEGRATION_SERVICES_HOST") : "demo-dev.lyricfinancial.com";
        boolean isSslOn = Boolean.parseBoolean(getParam(request, "ssl", System.getenv("DEFAULT_SSL_FLAG")));
        int defaultPort = isSslOn ? 443 : 80;

        HttpClientOptions options = new HttpClientOptions(new JsonObject().put("defaultPort", defaultPort).put("defaultHost", host)).setSsl(isSslOn);
        options.setLogActivity(true);
        if(isSslOn){
            PfxOptions pfkKeyCertOptions = new PfxOptions();
            PemTrustOptions pemOptions = new PemTrustOptions();

            final String env = System.getenv("API_ENV");
            Buffer certificate = getCert(String.format("%s/certificate.pfx", env));
            Buffer intermediateCertificate = getCert(String.format("%s/intermediate.pem", env));
            Buffer rootCertificate = getCert(String.format("%s/root.pem", env));

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
        String vendorId = getParam(req, "vendor-id", System.getenv("DEFAULT_VENDOR_ID"));
        cReq.putHeader("vendor-id", vendorId);

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

    protected Buffer generateMultipart(HttpServerRequest req, JsonObject client, JsonObject options, HttpClientRequest cReq) {
        Buffer body = Buffer.buffer();
        final boolean useJose = Boolean.parseBoolean(getParam(req, "jose", System.getenv("DEFAULT_JOSE_FLAG")));

        JsonObject fileData = new JsonObject();
        try {
            ClientRepository.getRoyaltyEarnings(fileData, options, client);
        } catch (IOException e) {
            logger.error(String.format("Error getting csv data: %s", e.getMessage()));
        }
        if(fileData.getBinary("data") != null){
            String contentDisposition = "Content-Disposition: form-data; name=\"FinancialRecordGroupingFileSet\"; filename=\"" + fileData.getString("filename") + "\"\n";
            final String contentType = fileData.getString("contentType") + "; lyric-fileset-file-type=songSummary; lyric-csv-schema=TunecoreDistributionSample";
            cReq.putHeader("lyric-csv-use-header","true");
            //cReq.putHeader("lyric-csv-use-header","false");
            //cReq.putHeader("lyric-csv-date-format-string", "yyyy-MM-dd HH:mm:ss");

            try {
                addDataToBuffer(body, contentDisposition, fileData.getBinary("data"), contentType, useJose);
            } catch (JoseException e) {
                thowSignEncryptError(req);
            }
        }
        else{
            cReq.putHeader("no-new-financial-records", "true");
        }

        String contentDisposition = "Content-Disposition: form-data; name=\"RegistrationRequest\"\n";
        try {
            addDataToBuffer(body, contentDisposition, client.toString().getBytes(), "application/json", useJose);
        } catch (JoseException e) {
            thowSignEncryptError(req);
        }
        body.appendString("--" + BOUNDARY + "--\n");

        return body;
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



    protected void addDataToBuffer(Buffer buffer, byte[] content, JsonObject multipartDetails, boolean useJose) throws JoseException {

        String data = String.valueOf(content);
        if(useJose) {
            data = signAndEncrypt(content, multipartDetails.getString("contentType"));
            multipartDetails.put("contentType", "application/jose");
        }

        buffer.appendString("--" + BOUNDARY + "\n");
        buffer.appendString(multipartDetails.getString("contentDisposition"));
        buffer.appendString("Content-Type: " + multipartDetails.getString("contentType") + "\n");
        buffer.appendString("\n");
        buffer.appendString(data);
        buffer.appendString("\n");
    }

    protected void addDataToBuffer(Buffer buffer, String contentDisposition, byte[] content, String contentType, boolean useJose) throws JoseException {

        String data = String.valueOf(content);
        if(useJose) {
            data = signAndEncrypt(content, contentType);
            contentType = "application/jose";
        }

        buffer.appendString("--" + BOUNDARY + "\n");
        buffer.appendString(contentDisposition);
        buffer.appendString("Content-Type: " + contentType + "\n");
        buffer.appendString("\n");
        buffer.appendString(data);
        buffer.appendString("\n");
    }

}
