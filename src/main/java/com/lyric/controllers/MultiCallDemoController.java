package com.lyric.controllers;

import com.lyric.ClientRepository;
import com.lyric.FileDataRepository;
import com.lyric.SecurityService;
import com.lyric.models.FileOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.lang.JoseException;
import rx.Observable;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by amymadden on 7/26/16.
 */
public class MultiCallDemoController extends DemoBaseController{

    private final Vertx vertx;

    Logger logger = LoggerFactory.getLogger(MultiCallDemoController.class.getName());


    public MultiCallDemoController(Vertx vertx, SecurityService securityService) {
        super(securityService);
        this.vertx = vertx;
    }

    public void create(RoutingContext routingContext) {
        HttpServerRequest req = routingContext.request();
        final HttpServerResponse mainResponse = req.response();
        HttpClient httpClient = getHttpClient(req, vertx, null);

        //JsonObject client = routingContext.getBodyAsJson();
        JsonObject bodyData = routingContext.getBodyAsJson();
        String vendorId = getParam(req, "vendorId", System.getenv("DEFAULT_VENDOR_ID"));


        String clientId = getParam(req, "id", null);

        boolean noFileForClient = !FileDataRepository.fileExistsOnS3(clientId);

        JsonObject client = getClientData(clientId, bodyData, vendorId);

        final HttpClientRequest registrationReq = httpClient.post("/v1/clients.form", registrationResp -> {
            logger.info("Registration response: " + registrationResp.statusCode());

            registrationResp.bodyHandler(data -> {

                handleResponse(mainResponse, registrationResp, data.toString());

                JsonObject registrationRespData = getDecryptedResponse(data, vendorId);

                String memberToken = registrationRespData.getJsonObject("vendorAccount").getString("memberToken");
                final HttpClientRequest fileSetReq = httpClient.post("/v1/clients/" + memberToken + "/financialRecordGroupingFileSets.form", fileSetResp -> {
                    logger.info("File Set response: " + fileSetResp.statusCode());

                    fileSetResp.bodyHandler(fileSetData -> {
                        JsonObject fileSetRespData = getDecryptedResponse(fileSetData, vendorId);
                        logger.info("Decryped File Set Response: " + fileSetRespData);
                    });
                }).setChunked(true);

                Buffer body = getFileSetBody(req, client, vendorId);
                setAsyncHeaders(req, fileSetReq, vendorId);
                fileSetReq.end(body);

            });
        }).setChunked(true);

        setAsyncHeaders(req, registrationReq, vendorId);

        if(noFileForClient){
            registrationReq.putHeader("no-new-financial-records", "true");
        }

        Buffer body = getRegistrationBody(req, client, vendorId);

        registrationReq.end(body);
    }

    private void handleResponse(HttpServerResponse mainResponse, HttpClientResponse resp, String responseData) {
        logger.info(responseData);
//        timer.cancel();
        mainResponse.setStatusCode(resp.statusCode());
        mainResponse.headers().setAll(resp.headers());
        mainResponse.setChunked(true);
        mainResponse.end(responseData);
    }

    private void setAsyncHeaders(HttpServerRequest req, HttpClientRequest request, String vendorId) {
        setHeaders(request, req, vendorId);
        request.putHeader("content-type", String.format("multipart/form-data;boundary=%s", BOUNDARY));
    }

    private Buffer getRegistrationBody(HttpServerRequest req, JsonObject client, String vendorId) {
        Buffer body = Buffer.buffer();

        final boolean useJose = Boolean.parseBoolean(getParam(req, "jose", System.getenv("DEFAULT_JOSE_FLAG")));

        JsonObject multipartDetails = getRegistrationRequestMultipartDetails();
        try {
            addDataToBuffer(body, client.toString().getBytes(), multipartDetails, useJose, null, vendorId);
        } catch (JoseException e) {
            thowSignEncryptError(req);
        }
        body.appendString("--" + BOUNDARY + "--\r\n");
        return body;
    }

    private Buffer getFileSetBody(HttpServerRequest req, JsonObject client, String vendorId) {
        Buffer body = Buffer.buffer();

        JsonArray fileRecords = FileDataRepository.getFileRecords(new FileOptions(new JsonObject().put("vendorType", "distributor")), client);

        final boolean useJose = Boolean.parseBoolean(getParam(req, "jose", System.getenv("DEFAULT_JOSE_FLAG")));

        // THIS WILL NEED TO BE UPDATED TO SUPPORT SENDING MULTIPLE FILES
        final JsonObject firstFileRecord = fileRecords.getJsonObject(0);
        JsonObject fileMultipartDetails = getFileMultipartDetails(firstFileRecord);
        try {
            addDataToBuffer(body, firstFileRecord.getBinary("data"), fileMultipartDetails, useJose, null, vendorId);
        } catch (JoseException e) {
            thowSignEncryptError(req);
        }
        body.appendString("--" + BOUNDARY + "--\r\n");
        return body;
    }

    private JsonObject getDecryptedResponse(Buffer data, String vendorId) {
        logger.info(data);
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
        return response;
    }


}
