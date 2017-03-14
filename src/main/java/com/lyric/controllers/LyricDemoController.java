package com.lyric.controllers;

import com.lyric.ClientRepository;
import com.lyric.FileDataRepository;
import com.lyric.SecurityService;
import com.lyric.models.FileOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
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
        // vendorType - distributor or publisher
        // frequencyInDays
        // numberOfPeriods
        // numberOfRecordsPerPeriod
    // options - if this json object is sent, then load the file data according to the options
        // contentType - this is the content type that we should use to make the call to the lyric server
        // royaltyEarningsContentType - this is the content type that we should be looking for to load/generate the file
        // filename - this is the name of the file that we should be loading to use for royalty earnings
    // vendorId - can control which vendor we should be making the api call for
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

        if(data.getString("vendorId", null) != null){
            vendorId = data.getString("vendorId");
        }

        logger.info("CREATING REGISTRATION FOR: VENDORID-" + vendorId + " VENDORCLIENTACCOUNTID-" + clientId);

        final JsonObject defaultFileOptions = new JsonObject().put("schemas", new JsonArray().add("TunecoreDistributionSample")).put("frequencyInDays", 30).put("numberOfPeriods", 12).put("getInteger", 6);
        final FileOptions fileOptions = new FileOptions(data.getJsonObject("fileOptions", defaultFileOptions));
        final JsonObject options = data.getJsonObject("options", new JsonObject());

        if(options.getString("filename") != null){
            fileOptions.setSpecifiedFileName(options.getString("filename"));
            fileOptions.setSpecifiedFileContentType(options.getString("royaltyEarningsContentType"));
        }

        JsonObject client = getClientData(clientId, data, vendorId);
        JsonArray fileRecords = FileDataRepository.getFileRecords(fileOptions, client);


        String contentTypeFromOptions = options.getString("contentType", "multipart/form-data");

        String uri = getUri(contentTypeFromOptions);
        HttpClientRequest cReq = getHttpClientRequest(req, uri, vertx, vendorId);

        setHeaders(cReq, req, vendorId);
        cReq.putHeader("no-new-financial-records", String.valueOf(fileRecords.isEmpty()));
        //cReq.putHeader("lyric-csv-use-header","true");  //this will need to change based on file options

        Buffer body;

        if(contentTypeFromOptions.equals("multipart/form-data")){
            body = handleMultipartRequest(req, useJose, client, fileRecords, cReq, vendorId);
        }
        else{
            //not supported right now
            body = handleJsonRequest(req, useJose, client, fileRecords, cReq, contentTypeFromOptions);
        }

        cReq.end(body);
    }

    private Buffer handleMultipartRequest(HttpServerRequest req, boolean useJose, JsonObject client, JsonArray fileRecords, HttpClientRequest cReq, String vendorId) {
        String contentType = String.format("multipart/form-data;boundary=%s", BOUNDARY);
        cReq.putHeader("content-type", contentType);

        Buffer body = null;
        try {
            body = generateMultipart(client, fileRecords, useJose, vendorId);
        } catch (JoseException e) {
            thowSignEncryptError(req);
        }

        return body;

    }

    private Buffer handleJsonRequest(HttpServerRequest req, boolean useJose, JsonObject client, JsonArray fileData, HttpClientRequest cReq, String contentTypeFromOptions) {
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

    private Buffer generateMultipart(JsonObject client, JsonArray fileRecords, boolean useJose, String vendorId) throws JoseException {
        Buffer body = Buffer.buffer();

        if(!fileRecords.isEmpty()){
            for (Object dataFile : fileRecords) {
                JsonObject dataFileObject = (JsonObject) dataFile;
                JsonObject fileMultipartDetails = getFileMultipartDetails(dataFileObject);
                addDataToBuffer(body, dataFileObject.getBinary("data"), fileMultipartDetails, useJose, dataFileObject.getJsonArray("additionalJweHeaders"), vendorId);
            }
        }

        JsonObject multipartDetails = getRegistrationRequestMultipartDetails();
        addDataToBuffer(body, client.toString().getBytes(), multipartDetails, useJose, null, vendorId);
        body.appendString("--" + BOUNDARY + "--\n");

        return body;
    }
}
