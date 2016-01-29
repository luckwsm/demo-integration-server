package com.lyric.test.integration;

import com.lyric.ApiServer;
import com.lyric.ClientRepository;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

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
            String token = resp.getHeader("ACCESS_TOKEN");
            context.assertNotNull(token);
            async.complete();
        });
        request.headers().set("content-type", "application/json");

        JsonObject clientInfo = ClientRepository.findClient(String.format("client%d", random));

        request.end(clientInfo.toString());
    }
}
