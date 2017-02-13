package com.lyric.test.integration;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by amymadden on 2/13/17.
 */
@RunWith(VertxUnitRunner.class)
public class ClientTests extends TestsBase {

    Logger logger = LoggerFactory.getLogger(ClientTests.class.getName());

    @Test
    public void testShouldClientData(TestContext context){

        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        int random = getRandomNumber();
        String randomEmail = random + "@email.com";

        final HttpClientRequest request = client.post(8080, "localhost", "/clients/" + random, resp -> {
            context.assertEquals(200, resp.statusCode());

            resp.bodyHandler(data -> {
                logger.info(data);
                JsonObject clientData = new JsonObject(data.toString());
                context.assertEquals(randomEmail, clientData.getJsonObject("userProfile").getJsonObject("user").getString("email"));
                async.complete();
            });

        });
        request.headers().set("content-type", "application/json");

        JsonObject fileOptions = new JsonObject()
                .put("email", randomEmail)
                .put("firstName", random + "First")
                .put("lastName", random + "Last");

        request.end(fileOptions.toString());
    }

}
