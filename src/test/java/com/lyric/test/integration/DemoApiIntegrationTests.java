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
        final HttpClientRequest request = client.post(8080, "localhost", "/members/123/advance", resp -> {

            context.assertEquals(200, resp.statusCode());
            resp.bodyHandler(body -> {
                JsonObject token = new JsonObject(body.toString());
                context.assertNotNull(token.getString("access_token"));
                async.complete();
            });
        });

        JsonObject clientInfo = new JsonObject()
                .put("firstName", "Test")
                .put("lastName", "User")
                .put("address1", "327 S 87 St")
                .put("email", "test71@email.com")
                .put("city", "Omaha")
                .put("state", "NE")
                .put("zipCode", "68123")
                .put("vendorClientAccountId", "ascaptest1260")
                .put("ssn", "333-44-5518")
                .put("phone", "2075554418")
                .put("mobilePhone", "2075556618")
                .put("bankName", "Bank of America")
                .put("bankAccountNumber", "12345678")
                .put("bankRoutingNumber", "211274450")
                .put("bankAccountType", "checking")
                .put("dob", "1967-01-01")
                ;


//                phone: '2075554493',
//                mobilePhone: '2075556693',
//                bankName: 'abc',
//                bankAccountNumber: '12345678',
//                bankRoutingNumber: '211274450',
//                bankAccountType: 'checking'

        request.end(clientInfo.toString());
    }
}
