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
import java.util.Random;
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

        router.post("/clients/:id/advance").handler(this::handleAdvanceRequest);

        final JsonObject serverOptions = new JsonObject();
        // Convert port to int
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

    private void handleAdvanceRequest(RoutingContext routingContext) {
        logger.info("INTO HANDLE ADVANCE REQUEST");
        HttpServerResponse response = routingContext.response();

        String clientId = routingContext.request().getParam("id");

        if(clientId == null){
            response.setStatusMessage("Member Id cannot be null.");
            response.setStatusCode(500).end();
        }

        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions(new JsonObject().put("defaultPort", 443).put("defaultHost", "api.lyricfinancial.com")).setSsl(true));

        /* Look up member data from your system */
        JsonObject client = findClient(clientId);

        logger.info(client);

        String contentType = routingContext.request().getHeader("content-type");

        HttpClientRequest request = httpClient.post("/vendorAPI/v1/json/clients", resp -> {
            if (resp.statusCode() != 201) {
                logger.error(String.format("An error occurred trying to register the member for an advance: %s", resp.statusMessage()));
                response.setStatusMessage(resp.statusMessage());
                response.setStatusCode(resp.statusCode()).end();
                return;
            }
            /* The POST to the registration API returns an ACCESS_TOKEN in the header.  This header
            is needed to pass back to the lyric-snippet so you need to send it back to your client.

            Also, a memberToken is returned in the body.  This token should be saved to your database
            so that any future calls will do updates to the system.
             */
            JsonObject obj = new JsonObject().put("access_token", resp.getHeader("ACCESS_TOKEN"));
            response.putHeader("content-type", "application/json").end(obj.encodePrettily());
        });

        /* 3 headers need to be set in order to call the Registration API.  vendorId, content-type
        and authorization.  vendorId and the username and password to create the credentials will be
        provided.  The content-type will depend on how the royalty data is being sent.
         */
        request.putHeader("vendorId", "ascap");
        request.putHeader("content-type", contentType);

        /* Username and password are used to generate the authorization header.  These values need to
        be base64 encoded to create the new authorization token.
         */
        String authToken = null;
        try {
            authToken = this.createCredentials("ascap", "WxjXgrzzGzrkPMv7hBFJ@PMkQX9e3e2N");
        } catch (UnsupportedEncodingException e) {
            logger.error("Could not create client credentials", e.getCause());
        }
        request.putHeader(HttpHeaders.AUTHORIZATION, "Basic " + authToken);

        request.end(client.toString());
    }

    /* This gets a unique user every time for testing purposes.  This would really be a lookup from
    your database.
     */
    private JsonObject findClient(String vendorClientAccountId){

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
                .put("ssn", String.format("333-44-%d", random))
                .put("phone", String.format("207555%d", random))
                .put("mobilePhone", String.format("207556%d", random))
                .put("bankName", "Bank of America")
                .put("bankAccountNumber", "12345678")
                .put("bankRoutingNumber", "211274450")
                .put("bankAccountType", "checking")
                .put("dob", "1967-01-01")
                ;

        return clientInfo;
    }

    private String createCredentials(String username, String password) throws UnsupportedEncodingException {
        return Base64.encodeBase64String(String.format("%s:%s", username, password).getBytes("UTF-8"));
    }
}
