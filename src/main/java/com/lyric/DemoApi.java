package com.lyric;

import com.google.common.collect.Sets;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.VoidHandler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.EnumUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;

/**
 * Created by amadden on 1/12/16.
 */
public class DemoApi extends AbstractVerticle {

    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";

    Logger logger = LoggerFactory.getLogger(DemoApi.class.getName());
    private static Set<HttpMethod> allHttpMethods = Sets.newConcurrentHashSet(EnumUtils.getEnumList(HttpMethod.class));
    private static Set<String> allowedCorsHeaders = Sets.newConcurrentHashSet(Arrays.asList("Accept", "Authorization", "Content-Type", "vendorId"));
    private CorsHandler corsHandler = CorsHandler.create("*").allowedMethods(allHttpMethods).allowedHeaders(allowedCorsHeaders).exposedHeader(ACCESS_TOKEN);

    @Override
    public void start(Future<Void> startFuture) {

        Router router = Router.router(vertx);

        router.route().handler(corsHandler);
        router.route().handler(CorsHandler.create("*"));

        router.route().handler(BodyHandler.create());

        router.post("/clients/:id/advance").handler(this::handleAdvanceRequest);

        final JsonObject serverOptions = new JsonObject();

        serverOptions.put("port", Integer.parseInt(System.getenv("PORT"))).put("host", "0.0.0.0");

        vertx.createHttpServer(new HttpServerOptions(serverOptions)).requestHandler(router::accept).listen(asyncResult -> {
            if (asyncResult.failed()) {
                startFuture.fail(asyncResult.cause());
                return;
            }
            logger.info("Http Server Started Successfully");
            startFuture.complete();
        });
    }

    private void handleAdvanceRequest(RoutingContext routingContext){
        HttpServerRequest req = routingContext.request();

        String clientId = routingContext.request().getParam("id");

        if(clientId == null){
            req.response().setStatusMessage("Client Id cannot be null.");
            req.response().setStatusCode(500).end();
            return;
        }

        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions(new JsonObject().put("defaultPort", 443).put("defaultHost", "api.lyricfinancial.com")).setSsl(true));

        if(isServerRequest(routingContext)){
            handleAdvanceRequestServer(routingContext, clientId, httpClient);
        }
        else{
            handleAdvanceRequestClient(routingContext, clientId, httpClient);
        }
    }

    private void handleAdvanceRequestClient(RoutingContext routingContext, String clientId, HttpClient httpClient){
        registerForAdvance(routingContext, routingContext.getBody(), httpClient);
    }

    private void handleAdvanceRequestServer(RoutingContext routingContext, String clientId, HttpClient httpClient){
        JsonObject options = routingContext.getBodyAsJson().getJsonObject("options");

        /* Look up client data from your system */
        JsonObject client = ClientRepository.findClient(clientId);

        Buffer body = Buffer.buffer();

        if(options.getString("contentType").equals("multipart/form-data")){
            generateMultipart(body, client, options);
            // remove content-type header because it came through as json
            // add multipart header  (can't do this because we don't have reference to request)
        }
        else{
            byte[] csvData = new byte[0];

            if(options.getString("royaltyEarningsContentType").equals("text/csv") && !options.getString("filename").equals("")){
                try {
                    csvData = ClientRepository.getRoyaltyEarnings(options.getString("filename"));
                } catch (IOException e) {
                    logger.error(String.format("Error getting csv data: %s", e.getMessage()));
                }
                String encodedCsvData = Base64.encodeBase64String(csvData);
                client.put("royaltyEarnings", encodedCsvData);
            }
            body.appendString(client.toString());
        }
        registerForAdvance(routingContext, body, httpClient);
    }

    private void generateMultipart(Buffer body, JsonObject client, JsonObject options){
        addClientToBuffer(client, body);
        addRoyaltyEarningsToBuffer(client, body, options);
    }

    private void addClientToBuffer(JsonObject client, Buffer buffer){
        buffer.appendString("--MyBoundary\r\n");
        buffer.appendString("Content-Disposition: form-data; name=\"clientData\";\r\n");
        buffer.appendString("Content-Type: text/plain\r\n");
        buffer.appendString("\r\n");
        buffer.appendString(client.toString());
        buffer.appendString("\r\n");
    }

    private void addRoyaltyEarningsToBuffer(JsonObject client, Buffer body, JsonObject options) {
        // ***JUST STUBBING OUT FOR NOW
        //check options to get filename and royaltyEarningsContentType, use this data to construct
        //the multipart data for royaltyEarnings

//            -        buffer.appendString("--MyBoundary\r\n");
//            -        buffer.appendString("Content-Disposition: form-data; name=\"royaltyEarnings\"; filename=\"" + fileName + "\"\r\n");
//            -        buffer.appendString("Content-Type: \"" + options.getString("royaltyEarningsContentType") + "\"\r\n");
//            -        buffer.appendString("\r\n");
//            -        buffer.appendBytes(bytes data from file);
//            -        buffer.appendString("\r\n");
    }



    private void registerForAdvance(RoutingContext routingContext, Buffer body, HttpClient httpClient){
        HttpServerRequest req = routingContext.request();

        String uri = getUri(req);
        HttpClientRequest cReq = httpClient.post(uri, cRes -> {
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

        setHeaders(cReq, req);
        cReq.setChunked(true);

        cReq.write(body);
        cReq.end();
    }

    private String getUri(HttpServerRequest req) {
        String uri = "/vendorAPI/v1/json/clients";

        String contentType = req.getHeader("content-type");
        if(contentType.substring(0, 9).equals("multipart")){
            uri = "/vendorAPI/v1/multipart/clients";
        }
        return uri;
    }

    private void setHeaders(HttpClientRequest cReq, HttpServerRequest req) {
        /* 3 headers need to be set in order to call the Registration API.  vendorId, content-type
        and authorization.  vendorId and the username and password to create the credentials will be
        provided to you.  The content-type will get copied from the server request.
        */
        req.headers().remove(HttpHeaders.HOST);
        cReq.headers().setAll(req.headers());

        cReq.putHeader("vendorId", "ascap");

        /* Username and password are used to generate the authorization header.  These values need to
        be base64 encoded to create the new authorization token.
         */
        String authToken = null;
        try {
            authToken = this.createCredentials("ascap", "WxjXgrzzGzrkPMv7hBFJ@PMkQX9e3e2N");
        } catch (UnsupportedEncodingException e) {
            logger.error("Could not create client credentials", e.getCause());
        }
        cReq.putHeader(HttpHeaders.AUTHORIZATION, "Basic " + authToken);
    }

    private String createCredentials(String username, String password) throws UnsupportedEncodingException {
        return Base64.encodeBase64String(String.format("%s:%s", username, password).getBytes("UTF-8"));
    }

    private boolean isServerRequest(RoutingContext routingContext){
        JsonObject body = routingContext.getBodyAsJson();
        return body.getJsonObject("options", null) != null;
    }
}
