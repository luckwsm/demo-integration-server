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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Ignore;
import java.util.Random;

/**
 * Created by amadden on 1/12/16.
 */
@Ignore
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

    @Test
    public void testWithDataShouldReturnAccessToken(TestContext context){

        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        int random = getRandomNumber();

        final HttpClientRequest request = client.post(8080, "localhost", "/clients/" + random + "/advance_client", resp -> {
            context.assertEquals(200, resp.statusCode());
            String token = resp.getHeader("ACCESS_TOKEN");
            context.assertNotNull(token);
            async.complete();
        });
        request.headers().set("content-type", "application/json");

        JsonObject user = new JsonObject()
                .put("firstName", String.format("Test%d", random))
                .put("lastName", String.format("User%d", random))
                .put("address1", "327 S 87 St")
                .put("email", String.format("%s@email.com", random))
                .put("city", "Omaha")
                .put("state", "NE")
                .put("zipCode", "68123")
                .put("phone", String.format("207555%d", random))
                .put("mobilePhone", String.format("207556%d", random))
                .put("dob", "1967-01-01")
                .put("gender", "male")
                .put("maritalStatus", "single")
                ;

        JsonObject bankInfo = new JsonObject()
                .put("bankName", "Bank of America")
                .put("bankAccountNumber", "12345678")
                .put("bankRoutingNumber", "211274450")
                .put("bankAccountType", "checking")
                ;

        JsonObject taxInfo = new JsonObject()
                .put("taxEinTinSsn", String.format("333-44-%d", random))
                .put("tinType", "ssn")
                .put("memberBusinessType", "individual")
                ;

        JsonObject vendorAccount = new JsonObject()
                .put("vendorClientAccountId", String.format("client%d", random))
                ;

        JsonObject clientInfo = new JsonObject()
                .put("user", user)
                .put("vendorAccount", vendorAccount)
                .put("taxInfo", taxInfo)
                .put("bankInfo", bankInfo)
                ;

        request.end(clientInfo.toString());
    }

    @Test
    public void serverJsonCallWithCsvShouldReturnAccessToken(TestContext context){

        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        int random = getRandomNumber();

        final HttpClientRequest request = client.post(8080, "localhost", "/clients/" + random + "/advance_server", resp -> {
            context.assertEquals(200, resp.statusCode());
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
    public void serverMultipartCallWithCsvShouldReturnAccessToken(TestContext context){

        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        int random = getRandomNumber();

        final HttpClientRequest request = client.post(8080, "localhost", "/clients/" + random + "/advance_server", resp -> {
            context.assertEquals(200, resp.statusCode());
            String token = resp.getHeader("ACCESS_TOKEN");
            context.assertNotNull(token);
            async.complete();
        });
        request.headers().set("content-type", "application/json");

        JsonObject options = new JsonObject().put("options", new JsonObject()
                .put("contentType", "multipart/form-data")
                .put("royaltyEarningsContentType", "text/csv")
                .put("filename", "sample.csv"));
        request.end(options.toString());
    }

    private int getRandomNumber() {
        int START = 1000;
        int END = 9999;
        Random r = new Random();
        return r.nextInt((END - START) + 1) + START;
    }
}
