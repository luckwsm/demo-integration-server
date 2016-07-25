package com.lyric.controllers;

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

/**
 * Created by amadden on 1/29/16.
 */
public class ClientDemoController extends DemoBaseController {
    private final Vertx vertx;

    Logger logger = LoggerFactory.getLogger(ClientDemoController.class.getName());

    public ClientDemoController(Vertx vertx, SecurityService securityService) {
        super(securityService);
        this.vertx = vertx;
    }

    public void create(RoutingContext routingContext) {
        HttpServerRequest req = routingContext.request();

        String clientId = getParam(req, "id", null);

        if (clientId == null) {
            req.response().setStatusMessage("Client Id cannot be null.");
            req.response().setStatusCode(500).end();
            return;
        }

        String uri = getUri("multipart/form-data");
        HttpClientRequest cReq = getHttpClientRequest(req, uri, vertx);

        setHeaders(cReq, req);
        cReq.setChunked(true);

        JsonObject client = routingContext.getBodyAsJson();


        Buffer body = processMultipart(req, new JsonObject(), client, cReq);

        cReq.setChunked(true).end(body);
    }

}
