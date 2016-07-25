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
import org.apache.commons.logging.Log;
import org.jose4j.lang.JoseException;

/**
 * Created by amadden on 1/29/16.
 */
public class ServerDemoController extends DemoBaseController {
    private final Vertx vertx;

    Logger logger = LoggerFactory.getLogger(ServerDemoController.class.getName());


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
        String vendorId = getParam(req, "vendorId", System.getenv("DEFAULT_VENDOR_ID"));

        /* Look up client data from your system */
        JsonObject client = ClientRepository.findClient(clientId, false, vendorId);

        String contentTypeFromOptions = options.getString("contentType");

        String uri = getUri(contentTypeFromOptions);
        HttpClientRequest cReq = getHttpClientRequest(req, uri, vertx);


        if(contentTypeFromOptions.equals("multipart/form-data")){
            Buffer body = processMultipart(req, options, client, cReq);
            logger.info("START OF API CALL: " + System.currentTimeMillis());
            final String asyncTokenHeader = cReq.headers().get("ASYNC_TOKEN");
            logger.info("ASYNC TOKEN HEADER: " + asyncTokenHeader);
            cReq.end(body);
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
            String payload = client.toString();

            if(useJose){
                try {
                    payload = signAndEncrypt(payload.getBytes(), contentTypeFromOptions);
                } catch (JoseException e) {
                    thowSignEncryptError(req);
                }
            }

            cReq.end(payload);
        }




    }




}
