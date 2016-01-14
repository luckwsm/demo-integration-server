package com.lyric.test.integration;

import com.lyric.DemoApi;
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.Random;

/**
 * Created by amadden on 1/12/16.
 */
@RunWith(VertxUnitRunner.class)
public class DemoApiIntegrationTests {
    Vertx vertx;

    @Before
    public void setUp(TestContext context) throws Exception {
        vertx = Vertx.vertx();

        DeploymentOptions options = new DeploymentOptions();
        vertx.deployVerticle(DemoApi.class.getName(), options, context.asyncAssertSuccess(resp -> {
            System.out.println("SUCCESS");
        }));
    }

    @After
    public void tearDown(TestContext context) throws Exception {
        vertx.close(context.asyncAssertSuccess());
    }

    @Ignore
    @Test
    public void testShouldReturnAccessToken(TestContext context){

        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        int START = 1000;
        int END = 9999;
        Random r = new Random();
        int random = r.nextInt((END - START) + 1) + START;

        final HttpClientRequest request = client.post(8080, "localhost", "/clients/" + random + "/advance", resp -> {

            context.assertEquals(200, resp.statusCode());
            resp.bodyHandler(body -> {
                JsonObject token = new JsonObject(body.toString());
                context.assertNotNull(token.getString("access_token"));
                async.complete();
            });
        });

        request.end();
    }
}
