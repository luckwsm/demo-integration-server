package com.lyric;

import com.google.common.collect.Sets;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.apache.commons.lang3.EnumUtils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Set;
import org.apache.commons.codec.binary.Base64;

/**
 * Created by amadden on 1/12/16.
 */
public class DemoApi extends AbstractVerticle {

    Logger logger = LoggerFactory.getLogger(DemoApi.class.getName());
    private static Set<HttpMethod> allHttpMethods = Sets.newConcurrentHashSet(EnumUtils.getEnumList(HttpMethod.class));
    private static Set<String> allowedCorsHeaders = Sets.newConcurrentHashSet(Arrays.asList("Accept", "Authorization", "Content-Type", "vendorId"));
    private CorsHandler corsHandler = CorsHandler.create("*").allowedMethods(allHttpMethods).allowedHeaders(allowedCorsHeaders);

    @Override
    public void start(Future<Void> startFuture) {

        Router router = Router.router(vertx);

        router.route().handler(corsHandler);
        router.route().handler(CorsHandler.create("*"));

        router.route().handler(BodyHandler.create());

        router.post("/members/:id/advance").handler(this::handleAdvanceRequest);
        router.get("/test").handler(this::handleTest);


        //final JsonObject serverOptions = config().getJsonObject("serverOptions");
        final JsonObject serverOptions = new JsonObject();
        // Convert port to int
        serverOptions.put("port", 8080).put("host", "0.0.0.0");

        vertx.createHttpServer(new HttpServerOptions(serverOptions)).requestHandler(router::accept).listen(asyncResult -> {
            if (asyncResult.failed()) {
                startFuture.fail(asyncResult.cause());
                return;
            }
            logger.info("Http Server Started Successfully");
            startFuture.complete();
        });


    }

    private void handleTest(RoutingContext routingContext){
        JsonObject obj = new JsonObject().put("test", "yay");
        routingContext.response().putHeader("content-type", "application/json").end(obj.encodePrettily());
    }

    private void handleAdvanceRequest(RoutingContext routingContext) {
        logger.info("INTO HANDLE ADVANCE REQUEST");
        HttpServerResponse response = routingContext.response();

        HttpClient client = vertx.createHttpClient(new HttpClientOptions(new JsonObject().put("defaultPort", 443).put("defaultHost", "api.lyricfinancial.com")).setSsl(true));
        String userInfo = routingContext.getBodyAsString();


        HttpClientRequest request = client.post("/vendorAPI/v1/json/clients", resp -> {
            if (resp.statusCode() != 201) {
                logger.info("GOT ERROR BACK");
                response.setStatusMessage(resp.statusMessage());
                response.setStatusCode(resp.statusCode()).end();
                return;
            }
            logger.info("SUCCESS");
            JsonObject obj = new JsonObject().put("access_token", resp.getHeader("ACCESS_TOKEN"));
            response.putHeader("content-type", "application/json").end(obj.encodePrettily());
        });



        request.putHeader("vendorId", "ascap");
        request.putHeader("content-type", "application/json");

        String auth = null;
        try {
            auth = this.createCredentials("ascap", "WxjXgrzzGzrkPMv7hBFJ@PMkQX9e3e2N");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        request.putHeader(HttpHeaders.AUTHORIZATION, "Basic " + auth);

//        String authString = String.format("%s:%s", "ascap", "WxjXgrzzGzrkPMv7hBFJ@PMkQX9e3e2N");
//        String bytesEncoded = Base64.getEncoder().encodeToString(authString.getBytes());
//        request.putHeader("Authorization", String.format("Basic %s", bytesEncoded));



//        request.handler(resp -> {
//            JsonObject obj = new JsonObject().put("access_token", resp.getHeader("access-token"));
//            response.putHeader("content-type", "application/json").end(obj.encodePrettily());
//        }).exceptionHandler(throwable -> {
//            response.setStatusMessage(throwable.getMessage());
//            response.setStatusCode(500).end();
//        });



        request.end(userInfo);
    }

    private String createCredentials(String username, String password) throws UnsupportedEncodingException {
        return Base64.encodeBase64String(String.format("%s:%s", username, password).getBytes("UTF-8"));
    }
}
