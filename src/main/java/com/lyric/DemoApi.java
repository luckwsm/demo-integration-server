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
        logger.info("INTO HANDLE ADVANCE REQUEST");
        HttpServerRequest req = routingContext.request();

        String clientId = routingContext.request().getParam("id");

        if(clientId == null){
            req.response().setStatusMessage("Client Id cannot be null.");
            req.response().setStatusCode(500).end();
            return;
        }

        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions(new JsonObject().put("defaultPort", 443).put("defaultHost", "api.lyricfinancial.com")).setSsl(true));

        String uri = getUri(req);
        logger.info("URI: " + uri);
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
        });

        setHeaders(cReq, req);

        cReq.setChunked(true);

        /* See if there is data in the body, otherwise look up client data from your system */
        Buffer body = routingContext.getBody();
        if(body.length() == 0){
            body.appendString(findClient(clientId));
        }
        //logger.info("BODY: " + routingContext.getBody().toString());
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

    /* This gets a unique user every time for testing purposes.  This would really be a lookup from
    your database.
     */
    private String findClient(String vendorClientAccountId){

        int START = 1000;
        int END = 9999;
        Random r = new Random();
        int random = r.nextInt((END - START) + 1) + START;

        JsonObject clientInfo = new JsonObject()
                .put("firstName", String.format("Test%d", random))
                .put("lastName", String.format("User%d", random))
                .put("address1", "327 S 87 St")
                .put("email", String.format("%s@email.com", vendorClientAccountId))
                .put("city", "Omaha")
                .put("state", "NE")
                .put("zipCode", "68123")
                .put("vendorClientAccountId", vendorClientAccountId)
                .put("taxEinTinSsn", String.format("333-44-%d", random))
                .put("tinType", "ssn")
                .put("phone", String.format("207555%d", random))
                .put("mobilePhone", String.format("207556%d", random))
                .put("bankName", "Bank of America")
                .put("bankAccountNumber", "12345678")
                .put("bankRoutingNumber", "211274450")
                .put("bankAccountType", "checking")
                .put("dob", "1967-01-01");

        return clientInfo.toString();
    }

    private String createCredentials(String username, String password) throws UnsupportedEncodingException {
        return Base64.encodeBase64String(String.format("%s:%s", username, password).getBytes("UTF-8"));
    }
}
