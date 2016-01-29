package com.lyric.controllers;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

/**
 * Created by amadden on 1/29/16.
 */
public class ClientDemoController extends DemoBaseController {
    private final Vertx vertx;
    Logger logger = LoggerFactory.getLogger(ClientDemoController.class.getName());

    public ClientDemoController(Vertx vertx) {
        this.vertx = vertx;
    }

    public void create(RoutingContext routingContext){
        HttpServerRequest req = routingContext.request();

        String clientId = getParam(req, "id", null);

        if(clientId == null){
            req.response().setStatusMessage("Client Id cannot be null.");
            req.response().setStatusCode(500).end();
            return;
        }

        String uri = getUri(req.getHeader("content-type"));
        HttpClientRequest cReq = getHttpClientRequest(req, uri, vertx);

        setHeaders(cReq, req);
        cReq.setChunked(true);

        cReq.write(routingContext.getBody());
        cReq.end();
    }
}
