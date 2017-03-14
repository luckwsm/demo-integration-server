package com.lyric.test.integration;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by amymadden on 2/13/17.
 */
@RunWith(VertxUnitRunner.class)
public class LyricDemoTests extends TestsBase {

    Logger logger = LoggerFactory.getLogger(LyricDemoTests.class.getName());

    @Test
    public void testCanRegisterPublisherWithFileFromFileSystem(TestContext context){

        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        //int random = getRandomNumber();
        String random = "amyLyricTest";

        final HttpClientRequest request = client.post(8080, "localhost", "/clients/" + random + "/advance", resp -> {
            context.assertEquals(201, resp.statusCode());
            String token = resp.getHeader("access-token");
            context.assertNotNull(token);
            async.complete();
        });
        request.headers().set("content-type", "application/json");

        JsonObject data = new JsonObject();

        data.put("clientOptions", new JsonObject()
                .put("email", random + "@email.com")
                .put("firstName", random + "First")
                .put("lastName", random + "Last"));

        data.put("fileOptions", new JsonObject()
                .put("vendorType", "publisher"));

        data.put("vendorId", "demopublisher");

        request.end(data.toString());
    }

    @Test
    public void testCanRegisterDistributorWithFileFromFileSystem(TestContext context){

        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        //int random = getRandomNumber();
        String random = "amyLyricDistTest";

        final HttpClientRequest request = client.post(8080, "localhost", "/clients/" + random + "/advance", resp -> {
            context.assertEquals(201, resp.statusCode());
            String token = resp.getHeader("access-token");
            context.assertNotNull(token);
            async.complete();
        });
        request.headers().set("content-type", "application/json");

        JsonObject data = new JsonObject();

        data.put("clientOptions", new JsonObject()
                .put("email", random + "@email.com")
                .put("firstName", random + "First")
                .put("lastName", random + "Last"));

        data.put("fileOptions", new JsonObject()
                .put("vendorType", "distributor"));

        data.put("vendorId", "demodistributor");

        request.end(data.toString());
    }

    @Test
    public void testShouldRegisterUserWithFileFromFileSystem(TestContext context){

        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        int random = getRandomNumber();

        final HttpClientRequest request = client.post(8080, "localhost", "/clients/" + random + "/advance", resp -> {
            context.assertEquals(201, resp.statusCode());
            String token = resp.getHeader("access-token");
            context.assertNotNull(token);
            async.complete();
        });
        request.headers().set("content-type", "application/json");

        JsonObject data = new JsonObject();

        data.put("options", new JsonObject()
                .put("contentType", "multipart/form-data")
                .put("royaltyEarningsContentType", "text/csv")
                .put("filename", "943344.csv"));

        request.end(data.toString());
    }

    @Test
    @Ignore
    public void productionTest(TestContext context){

        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        String productionUserVendorClientAccountId = "productionSmokeTestUser";

        final HttpClientRequest request = client.post(8080, "localhost", "/clients/" + productionUserVendorClientAccountId + "/advance", resp -> {
            context.assertEquals(201, resp.statusCode());
            String token = resp.getHeader("access-token");
            context.assertNotNull(token);
            async.complete();
        });
        request.headers().set("content-type", "application/json");

        JsonObject options = new JsonObject().put("options", new JsonObject()
                .put("contentType", "multipart/form-data")
                .put("royaltyEarningsContentType", "text/csv")
                .put("filename", "943344.csv"));
        request.end(options.toString());
    }
}
