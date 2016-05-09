package com.lyric.controllers;

import com.lyric.ClientRepository;
import com.lyric.SecurityService;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.jose4j.lang.JoseException;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by amadden on 1/29/16.
 */
public class ServerDemoController extends DemoBaseController {
    private final Vertx vertx;

    Logger logger = LoggerFactory.getLogger(ServerDemoController.class.getName());
    private static String BOUNDARY = "----LyricBoundaryAL0lfjW6DJtKiwkd";

    public ServerDemoController(Vertx vertx, SecurityService securityService) {
        super(securityService);
        this.vertx = vertx;
    }

    public void create(RoutingContext routingContext){
        HttpServerRequest req = routingContext.request();
        final boolean useJose = Boolean.parseBoolean(getParam(req, "jose", System.getenv("DEFAULT_JOSE_FLAG")));

        String clientId = getParam(req, "id", null);

        if(clientId == null){
            req.response().setStatusMessage("Client Id cannot be null.");
            req.response().setStatusCode(500).end();
            return;
        }

        JsonObject options = routingContext.getBodyAsJson().getJsonObject("options");

        /* Look up client data from your system */
        JsonObject client = ClientRepository.findClient(clientId, false);

        Buffer body = Buffer.buffer();

        String contentTypeFromOptions = options.getString("contentType");

        String uri = getUri(contentTypeFromOptions, req);
        HttpClientRequest cReq = getHttpClientRequest(req, uri, vertx);



        if(contentTypeFromOptions.equals("multipart/form-data")){
            cReq.putHeader("content-type", contentTypeFromOptions);
            contentTypeFromOptions = String.format("%s;boundary=%s", contentTypeFromOptions, BOUNDARY);
            setHeaders(cReq, req);
            cReq.putHeader("content-type", contentTypeFromOptions);


            try {
                generateMultipart(body, client, options, useJose);

            } catch (JoseException e) {
                thowSignEncryptError(req);
            }
        }
        else{
            setHeaders(cReq, req);
//            if(shouldLoadRoyaltyEarningsCsv(options)){
//                byte[] csvData = new byte[0];
//                try {
//                    csvData = ClientRepository.getRoyaltyEarnings(options.getString("filename"));
//                } catch (IOException e) {
//                    logger.error(String.format("Error getting csv data: %s", e.getMessage()));
//                }
//                String encodedCsvData = Base64.encodeBase64String(csvData);
//                client.put("royaltyEarnings", encodedCsvData);
//            }
            body.appendString(client.toString());


            if(useJose){
                try {
                    body = Buffer.buffer(signAndEncrypt(body.getBytes(), contentTypeFromOptions));
                } catch (JoseException e) {
                    thowSignEncryptError(req);
                }
            }


        }

        cReq.setChunked(true);

        cReq.end(body);
    }

    private void generateMultipart(Buffer body, JsonObject client, JsonObject options, boolean useJose) throws JoseException {
        if(shouldLoadRoyaltyEarningsCsv(options)){
            byte[] csvData = new byte[0];
            try {
                csvData = ClientRepository.getRoyaltyEarnings(options.getString("filename"));
            } catch (IOException e) {
                logger.error(String.format("Error getting csv data: %s", e.getMessage()));
            }
            addRoyaltyEarningsToBuffer(body, options, csvData, useJose);
        }
        addClientToBuffer(client, body, useJose);
        body.appendString("--" + BOUNDARY + "--\r\n");
    }

    private void addClientToBuffer(JsonObject client, Buffer buffer, boolean useJose) throws JoseException {
        String payload = client.toString();
        String contentType = "application/json";
        if(useJose){
            payload = signAndEncrypt(client.toString().getBytes(),  "application/json");
            contentType = "application/jose";
        }

        String contentDisposition = "Content-Disposition: form-data; name=\"UserProfile\"\r\n";
        addDataToBuffer(buffer, contentDisposition, payload, contentType);
    }

    private void addRoyaltyEarningsToBuffer(Buffer buffer, JsonObject options, byte[] royaltyEarningsData, boolean useJose) throws JoseException {
        final String royaltyEarningsContentType = options.getString("royaltyEarningsContentType");
        String payload = Arrays.toString(royaltyEarningsData);
        String contentType = "application/json";
        if(useJose) {
            payload = signAndEncrypt(royaltyEarningsData, royaltyEarningsContentType);
            contentType = "application/jose";
        }

        String contentDisposition = "Content-Disposition: form-data; name=\"DistributionGrouping\"; filename=\"" + options.getString("filename") + "\"\r\n";
        addDataToBuffer(buffer, contentDisposition, payload, contentType);
    }

    private void addDataToBuffer(Buffer buffer, String contentDisposition, String payload, String contentType){
        buffer.appendString("--" + BOUNDARY + "\r\n");
        buffer.appendString(contentDisposition);
        buffer.appendString("Content-Type: " + contentType + "\r\n");
        buffer.appendString("\r\n");
        buffer.appendString(payload);
        buffer.appendString("\r\n");
    }
}
