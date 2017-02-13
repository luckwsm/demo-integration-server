package com.lyric.controllers;

import com.lyric.ClientRepository;
import com.lyric.DataGenerator;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by amymadden on 2/13/17.
 */
public class ClientController {
    private final Vertx vertx;

    Logger logger = LoggerFactory.getLogger(FileDataController.class.getName());

    public ClientController(Vertx vertx) {
        this.vertx = vertx;
    }

    // Used internally for our demos
    public void getClient(RoutingContext routingContext) {
        HttpServerRequest req = routingContext.request();
        JsonObject clientOptions = routingContext.getBodyAsJson();
        String clientId = getParam(req, "id", null);
        String vendorId = getParam(req, "vendorId", System.getenv("DEFAULT_VENDOR_ID"));

        JsonObject client = ClientRepository.findClient(clientId, false, vendorId, clientOptions);

        final HttpServerResponse response = routingContext.response();
        response.end(client.toString());
    }

    protected String getParam(HttpServerRequest request, String paramName, String defaultValue) {
        String param = request.getParam(paramName);
        if (StringUtils.isBlank(param)) {
            return defaultValue;
        }
        return param;
    }
}
