package com.lyric.test.integration;

import com.lyric.ClientRepository;
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
public class ClientDemoTests extends TestsBase{

    @Test
    public void testShouldReturnAccessToken(TestContext context){

        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        int random = getRandomNumber();

        final HttpClientRequest request = client.post(8080, "localhost", "/clients/" + random + "/advance_client", resp -> {
            context.assertEquals(201, resp.statusCode());
            String token = resp.getHeader("access-token");
            context.assertNotNull(token);
            async.complete();
        });
        request.headers().set("content-type", "application/json");

        JsonObject clientInfo = ClientRepository.findClient(String.format("client%d", random), false, "demo");

        request.end(clientInfo.toString());
    }
}
