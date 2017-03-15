package com.lyric.controllers;

import com.lyric.ClientRepository;
import com.lyric.FileDataRepository;
import com.lyric.models.FileOptions;
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
 * Created by amymadden on 2/11/17.
 */
public class FileDataController {
    private final Vertx vertx;

    Logger logger = LoggerFactory.getLogger(FileDataController.class.getName());

    public FileDataController(Vertx vertx) {
        this.vertx = vertx;
    }

    // Used internally for our demos
    public void getFileData(RoutingContext routingContext) {
        HttpServerRequest req = routingContext.request();
        JsonObject options = routingContext.getBodyAsJson();
        String clientId = getParam(req, "id", null);
        String vendorId = getParam(req, "vendorId", System.getenv("DEFAULT_VENDOR_ID"));

        JsonObject client = ClientRepository.findClient(clientId, false, vendorId, options.getJsonObject("clientOptions"));

        JsonArray fileData = new JsonArray();

        final FileOptions fileOptions = new FileOptions(options.getJsonObject("fileOptions"));

        final String schema = fileOptions.getSchemas()[0];
        fileData = FileDataRepository.getFileRecordsJson(schema, fileOptions, client);

        final HttpServerResponse response = routingContext.response();
        response.end(fileData.toString());
    }

    protected String getParam(HttpServerRequest request, String paramName, String defaultValue) {
        String param = request.getParam(paramName);
        if (StringUtils.isBlank(param)) {
            return defaultValue;
        }
        return param;
    }
}
