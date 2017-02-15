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

/**
 * Created by amymadden on 2/14/17.
 */
public class LyricDemoController extends DemoBaseController {
    private final Vertx vertx;

    Logger logger = LoggerFactory.getLogger(LyricDemoController.class.getName());

    public LyricDemoController(Vertx vertx, SecurityService securityService) {
        super(securityService);
        this.vertx = vertx;
    }

    // Body is a JsonObject that could have various JsonObject properties depending on configuration
    // client - if this json object is sent, then this is the data that should be sent to the server for registration
    // clientOptions - if this json object is sent, then create the user object based on this data to send to the server for registration
    // fileOptions - if this json object is sent, then dynamically create a csv file based on these options to send as file data during registration
    // options - if this json object is sent, then load the file data according to the options
    public void create(RoutingContext routingContext){
        HttpServerRequest req = routingContext.request();
        final boolean useJose = Boolean.parseBoolean(getParam(req, "jose", System.getenv("DEFAULT_JOSE_FLAG")));

        String clientId = getParam(req, "id", null);

        if(clientId == null){
            req.response().setStatusMessage("Client Id cannot be null.");
            req.response().setStatusCode(500).end();
            return;
        }

        JsonObject data = routingContext.getBodyAsJson();
        String vendorId = getParam(req, "vendorId", System.getenv("DEFAULT_VENDOR_ID"));
        final JsonObject fileOptions = data.getJsonObject("fileOptions", null);
        final JsonObject options = data.getJsonObject("options", new JsonObject());

        JsonObject client = getClientData(clientId, data, vendorId);
        JsonObject fileData = getFileData(client, fileOptions, options);


        String contentTypeFromOptions = options.getString("contentType", "multipart/form-data");

        String uri = getUri(contentTypeFromOptions);
        HttpClientRequest cReq = getHttpClientRequest(req, uri, vertx);

        setHeaders(cReq, req);
        cReq.putHeader("no-new-financial-records", String.valueOf(fileData.getBinary("data") == null));
        cReq.putHeader("lyric-csv-use-header","true");

        Buffer body;

        if(contentTypeFromOptions.equals("multipart/form-data")){
            body = handleMultipartRequest(req, useJose, client, fileData, cReq);
        }
        else{
            //not supported right now
            body = handleJsonRequest(req, useJose, client, fileData, cReq, contentTypeFromOptions);
        }

        cReq.end(body);
    }

    private Buffer handleMultipartRequest(HttpServerRequest req, boolean useJose, JsonObject client, JsonObject fileData, HttpClientRequest cReq) {
        String contentType = String.format("multipart/form-data;boundary=%s", BOUNDARY);
        cReq.putHeader("content-type", contentType);

        Buffer body = null;
        try {
            body = generateMultipart(client, fileData, useJose);
        } catch (JoseException e) {
            thowSignEncryptError(req);
        }

        return body;

    }

    private Buffer handleJsonRequest(HttpServerRequest req, boolean useJose, JsonObject client, JsonObject fileData, HttpClientRequest cReq, String contentTypeFromOptions) {
        req.response().setStatusMessage("Json requests are not supported at this time.");
        req.response().setStatusCode(500).end();

        return null;
//        String payload = client.toString();
//
//        if(useJose){
//            try {
//                payload = signAndEncrypt(payload.getBytes(), contentTypeFromOptions);
//            } catch (JoseException e) {
//                thowSignEncryptError(req);
//            }
//        }
//
//
//        cReq.end(payload);
    }

    private JsonObject getFileData(JsonObject client, JsonObject fileOptions, JsonObject options) {
        JsonObject fileData = new JsonObject();
        if(fileOptions != null){
            fileData = ClientRepository.generateRoyaltyEarnings(fileOptions, client);
        }
        else{
            try {

                ClientRepository.getRoyaltyEarnings(fileData, options, client);
            } catch (IOException e) {
                logger.error(String.format("Error getting csv data: %s", e.getMessage()));
            }
        }
        return fileData;
    }

    private JsonObject getClientData(String clientId, JsonObject data, String vendorId) {
        JsonObject client = data.getJsonObject("client", null);

        if(client == null){
            JsonObject clientOptions = data.getJsonObject("clientOptions", null);

            /* Look up client data from your system */
            client = ClientRepository.findClient(clientId, false, vendorId, clientOptions);
        }
        return client;
    }

    private Buffer generateMultipart(JsonObject client, JsonObject fileData, boolean useJose) throws JoseException {
        Buffer body = Buffer.buffer();

        if(fileData.getBinary("data") != null){
            JsonObject fileMultipartDetails = getFileMultipartDetails(fileData);
            addDataToBuffer(body, fileData.getBinary("data"), fileMultipartDetails, useJose);
        }

        JsonObject multipartDetails = getRegistrationRequestMultipartDetails();
        addDataToBuffer(body, client.toString().getBytes(), multipartDetails, useJose);
        body.appendString("--" + BOUNDARY + "--\n");

        return body;
    }
}
