package com.lyric.controllers;

import com.lyric.ClientRepository;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;

/**
 * Created by amadden on 1/29/16.
 */
public class ServerDemoController extends DemoBaseController {
    private final Vertx vertx;
    Logger logger = LoggerFactory.getLogger(ServerDemoController.class.getName());
    private static String BOUNDARY = "----LyricBoundaryAL0lfjW6DJtKiwkd";

    public ServerDemoController(Vertx vertx) {
        this.vertx = vertx;
    }

    public void create(RoutingContext routingContext){
        HttpServerRequest req = routingContext.request();

        String clientId = getParam(req, "id", null);

        if(clientId == null){
            req.response().setStatusMessage("Client Id cannot be null.");
            req.response().setStatusCode(500).end();
            return;
        }



        JsonObject options = routingContext.getBodyAsJson().getJsonObject("options");

        /* Look up client data from your system */
        JsonObject client = ClientRepository.findClient(clientId);

        Buffer body = Buffer.buffer();

        String contentType = options.getString("contentType");

        if(contentType.equals("multipart/form-data")){
            contentType = String.format("%s;boundary=%s", contentType, BOUNDARY);
            generateMultipart(body, client, options);
        }
        else{
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
        }

        String uri = getUri(contentType);
        HttpClientRequest cReq = getHttpClientRequest(req, uri, vertx);

        setHeaders(cReq, req);
        cReq.putHeader("content-type", contentType);

        cReq.setChunked(true);

        cReq.end(body);
    }

    private void generateMultipart(Buffer body, JsonObject client, JsonObject options){
        if(shouldLoadRoyaltyEarningsCsv(options)){
            byte[] csvData = new byte[0];
            try {
                csvData = ClientRepository.getRoyaltyEarnings(options.getString("filename"));
            } catch (IOException e) {
                logger.error(String.format("Error getting csv data: %s", e.getMessage()));
            }
            addRoyaltyEarningsToBuffer(body, options, csvData);
        }
        addClientToBuffer(client, body);
        body.appendString("--" + BOUNDARY + "--\r\n");
    }

    private void addClientToBuffer(JsonObject client, Buffer buffer){
        buffer.appendString("--" + BOUNDARY + "\r\n");
        buffer.appendString("Content-Disposition: form-data; name=\"clientData\"\r\n");
        buffer.appendString("\r\n");
        buffer.appendString(client.toString());
        buffer.appendString("\r\n");
    }

    private void addRoyaltyEarningsToBuffer(Buffer buffer, JsonObject options, byte[] royaltyEarningsData) {
        buffer.appendString("--" + BOUNDARY + "\r\n");
        buffer.appendString("Content-Disposition: form-data; name=\"royaltyEarnings\"; filename=\"" + options.getString("filename") + "\"\r\n");
        buffer.appendString("Content-Type: " + options.getString("royaltyEarningsContentType") + "\r\n");
        buffer.appendString("\r\n");
        buffer.appendBytes(royaltyEarningsData);
        buffer.appendString("\r\n");
    }
}
