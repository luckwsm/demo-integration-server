package com.lyric.test.integration;

import com.lyric.ClientRepository;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

/**
 * Created by amymadden on 7/26/16.
 */
@RunWith(VertxUnitRunner.class)
public class MultiCallDemoTests extends TestsBase{

    Logger logger = LoggerFactory.getLogger(MultiCallDemoTests.class.getName());
    boolean PINGED = false;

    @Test
    public void justRegistrationShouldntHitPing(TestContext context){
        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        int random = getRandomNumber();

        final HttpClientRequest request = client.post(8080, "localhost", "/clients/" + random + "/advance_multi", resp -> {
            logger.info("Status code: " + resp.statusCode());
            context.assertEquals(201, resp.statusCode());

            resp.bodyHandler(data -> {
                logger.info(data);
            });
            String token = resp.getHeader("access-token");
            context.assertNotNull(token);
            context.assertFalse(PINGED);
            async.complete();
        }).setChunked(true);

        request.continueHandler(resp -> {
            logger.info("Ping");
            PINGED = true;
        });
        request.headers().set("content-type", "application/json");

        JsonObject clientInfo = ClientRepository.findClient(String.format("client%d", random), false, "demo");

        request.end(clientInfo.toString());
    }

    @Test(timeout = 10*60*1000L)
    public void separateRegistrationAndFileCallWillSendPingBack(TestContext context){
        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        JsonObject clientInfo = getKnownClient();
        String vendorClientAccountId = clientInfo.getJsonObject("userProfile").getJsonObject("vendorAccount").getString("vendorClientAccountId");

        final HttpClientRequest request = client.post(8080, "localhost", "/clients/" + vendorClientAccountId + "/advance_multi", resp -> {
            logger.info("Status code: " + resp.statusCode());
            context.assertEquals(201, resp.statusCode());

            resp.bodyHandler(data -> {
                logger.info(data);
            });
            String token = resp.getHeader("access-token");
            context.assertNotNull(token);
            context.assertTrue(PINGED);
            async.complete();
        }).setChunked(true);

        request.continueHandler(resp -> logger.info("Ping"));
        request.headers().set("content-type", "application/json");


//        vertx.setTimer(5*60*1000L, event -> {
//            context.fail("timed out");
//        });



        request.end(clientInfo.toString());
    }

    public static JsonObject getKnownClient(){

        int START = 1000;
        int END = 9999;
        Random r = new Random();
        int random = r.nextInt((END - START) + 1) + START;

        JsonObject user = new JsonObject()
                .put("firstName", String.format("Amy", random))
                .put("lastName", String.format("Test4", random))
                .put("email", String.format("amyTest4@email.com", random))
                ;

        JsonObject vendorAccount = new JsonObject()
                .put("vendorClientAccountId", "amyTest4")
                .put("vendorId", "demo")
                .put("memberSince", "2007-01-01")
                ;

        JsonObject userProfile = new JsonObject()
                .put("user", user)
                .put("vendorAccount", vendorAccount)
                ;

        return new JsonObject()
                .put("userProfile", userProfile);
    }
}
