package com.lyric.test.integration;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by amadden on 1/29/16.
 */
@RunWith(VertxUnitRunner.class)
public class ServerDemoTests extends TestsBase{

    @Test
    public void jsonCallWithCsvShouldReturnAccessToken(TestContext context){

        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        int random = getRandomNumber();

        final HttpClientRequest request = client.post(8080, "localhost", "/clients/" + random + "/advance_server", resp -> {
            context.assertEquals(201, resp.statusCode());
            String token = resp.getHeader("ACCESS_TOKEN");
            context.assertNotNull(token);
            async.complete();
        });
        request.headers().set("content-type", "application/json");

        JsonObject options = new JsonObject().put("options", new JsonObject()
                .put("contentType", "application/json")
                .put("royaltyEarningsContentType", "text/csv")
                .put("filename", "sample.csv"));
        request.end(options.toString());
    }

    @Test
    public void multipartCallWithCsvShouldReturnAccessToken(TestContext context){

        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        int random = getRandomNumber();

        final HttpClientRequest request = client.post(8080, "localhost", "/clients/" + random + "/advance_server", resp -> {
            context.assertEquals(201, resp.statusCode());
            String token = resp.getHeader("ACCESS_TOKEN");
            context.assertNotNull(token);
            async.complete();
        });
        request.headers().set("content-type", "application/json");

        JsonObject options = new JsonObject().put("options", new JsonObject()
                .put("contentType", "multipart/form-data")
                .put("royaltyEarningsContentType", "text/csv")
                .put("filename", ""));
        request.end(options.toString());
    }
}
