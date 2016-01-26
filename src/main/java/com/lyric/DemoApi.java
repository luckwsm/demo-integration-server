package com.lyric;

import com.google.common.collect.Sets;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
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
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
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
    private static String BOUNDARY = "----LyricBoundaryAL0lfjW6DJtKiwkd";

    @Override
    public void start(Future<Void> startFuture) {

        Router router = Router.router(vertx);

        router.route().handler(corsHandler);
        router.route().handler(CorsHandler.create("*"));

        router.route().handler(BodyHandler.create());

        router.post("/clients/:id/advance_client").handler(this::handleAdvanceRequestClient);
        router.post("/clients/:id/advance_server").handler(this::handleAdvanceRequestServer);

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

    private void handleAdvanceRequestClient(RoutingContext routingContext){
        logger.info("INTO HANDLE ADVANCE REQUEST CLIENT");
        HttpServerRequest req = routingContext.request();

        String clientId = getParam(req, "id", null);

        if(clientId == null){
            req.response().setStatusMessage("Client Id cannot be null.");
            req.response().setStatusCode(500).end();
            return;
        }

        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions(new JsonObject().put("defaultPort", 443).put("defaultHost", "api.lyricfinancial.com")).setSsl(true));

        String uri = getUri(req.getHeader("content-type"));
        HttpClientRequest cReq = getHttpClientRequest(req, httpClient, uri);

        setHeaders(cReq, req);
        cReq.setChunked(true);

        cReq.write(routingContext.getBody());
        cReq.end();
    }

    private void handleAdvanceRequestServer(RoutingContext routingContext){
        HttpServerRequest req = routingContext.request();

        String clientId = getParam(req, "id", null);

        if(clientId == null){
            req.response().setStatusMessage("Client Id cannot be null.");
            req.response().setStatusCode(500).end();
            return;
        }

        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions(new JsonObject().put("defaultPort", 443).put("defaultHost", "api.lyricfinancial.com")).setSsl(true));

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
        HttpClientRequest cReq = getHttpClientRequest(req, httpClient, uri);

        setHeaders(cReq, req);
        cReq.putHeader("content-type", contentType);

        cReq.setChunked(true);

        cReq.end(body);
    }

    private HttpClientRequest getHttpClientRequest(HttpServerRequest req, HttpClient httpClient, String uri) {
        return httpClient.post(uri, cRes -> {
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
    }

    private boolean shouldLoadRoyaltyEarningsCsv(JsonObject options) {
        return options.getString("royaltyEarningsContentType").equals("text/csv") && !options.getString("filename").equals("");
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
        //buffer.a
        buffer.appendBytes(royaltyEarningsData);
        buffer.appendString("\r\n");
    }

    private String getUri(String contentType) {
        String uri = "/vendorAPI/v1/clients.json";

        if(contentType.substring(0, 9).equals("multipart")){
            uri = "/vendorAPI/v1/clients.form";
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

        setAuthorizationHeaders(cReq, req);
    }

    private void setAuthorizationHeaders(HttpClientRequest cReq, HttpServerRequest req) {
        String vendorId = getParam(req, "vendorId", System.getenv("DEFAULT_VENDOR_ID"));
        logger.info("********VENDOR ID: " + vendorId);
        cReq.putHeader("vendorId", vendorId);

        /* Username and password are used to generate the authorization header.  These values need to
        be base64 encoded to create the new authorization token.
         */
        String authToken = null;
        try {
            String username = getParam(req, "username", System.getenv("DEFAULT_USERNAME"));
            logger.info("**********USERNAME: " + username);
            String password = getParam(req, "password", System.getenv("DEFAULT_PASSWORD"));
            authToken = this.createCredentials(username, password);
        } catch (UnsupportedEncodingException e) {
            logger.error("Could not create client credentials", e.getCause());
        }
        cReq.putHeader(HttpHeaders.AUTHORIZATION, "Basic " + authToken);
    }

    private String createCredentials(String username, String password) throws UnsupportedEncodingException {
        return Base64.encodeBase64String(String.format("%s:%s", username, password).getBytes("UTF-8"));
    }

    private String getParam(HttpServerRequest request, String paramName, String defaultValue) {
        String param = request.getParam(paramName);
        if (StringUtils.isBlank(param)) {
            return defaultValue;
        }
        return param;
    }
}
