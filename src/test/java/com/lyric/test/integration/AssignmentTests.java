package com.lyric.test.integration;

import com.lyric.SecurityService;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;

/**
 * Created by amadden on 1/29/16.
 */
@RunWith(VertxUnitRunner.class)
public class AssignmentTests extends TestsBase{

    @Test
    public void testShouldReturnEncryptedResponse(TestContext context) {
        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        int random = getRandomNumber();

        final HttpClientRequest request = client.post(8080, "localhost", "/clients/" + random + "/assignments", resp -> {
            context.assertEquals(200, resp.statusCode());

            assertAssignments(async, random, client, context);
           // async.complete();
        });
        request.headers().set("content-type", "application/json");

        JsonObject assignment = new JsonObject()
                .put("memberToken", "member-" + random)
                .put("firstName", "First" + random)
                .put("lastName", "Last" + random)
                .put("assignToLyric", true)
                .put("amount", 5000)
                .put("assignmentDate", LocalDateTime.now().toString());


        try {
            final JsonObject jsonKey = getLyricKeys();
            final RsaJsonWebKey lyricRsaJsonWebKey = (RsaJsonWebKey) JsonWebKey.Factory.newJwk(jsonKey.toString());

            final JsonObject vendorKey = getVendorKey();
            final RsaJsonWebKey vendorRsaJsonWebKey = (RsaJsonWebKey) JsonWebKey.Factory.newJwk(vendorKey.toString());

            SecurityService securityService = new SecurityService(vendorRsaJsonWebKey, lyricRsaJsonWebKey);

            JsonWebSignature signedPayload = securityService.createSignature(assignment.toString().getBytes());
            String encryptedPayload = securityService.encryptPayload(signedPayload, assignment.toString().getBytes(), "application/json");

            request.end(encryptedPayload);
        }
        catch (JoseException e) {
            e.printStackTrace();
        }

    }

    private void assertAssignments(Async async, Integer vendorClientAccountId, HttpClient client, TestContext context) {
        final HttpClientRequest request = client.get(8080, "localhost", "/clients/" + vendorClientAccountId + "/assignments", resp -> {
            context.assertEquals(200, resp.statusCode());

            resp.bodyHandler(body -> {
                JsonArray assignments = new JsonArray(body.toString());
                context.assertEquals(1, assignments.size());
                async.complete();
            });

        });
        request.headers().set("content-type", "application/json");


        request.end();
    }

    private JsonObject getLyricKeys(){
        return new JsonObject("{\n" +

                "      \"alg\": \"RS256\",\n" +
                "      \"d\": \"rD4nLCsUBaiWNZyCIrV7m3FGTZVV5b45-OOzgYHrCH-auI8A-WuZ09NFeO1DtGZjNGDGcJyGWkDMiLTC9T7xMqI6yrl0GWdW_ldtjTpE4B1tSQL3XJqDZtx8EfOk9E5wpUpjez0WR7qoSpnKxViB7UXmt8Apd7ARqpjQrhDpwyOYzk1k6JsYJSGEa9Gur8iM70Iwh4h3ppKMD5IV-OmPB2Oi_w4tMD88Ggfo3z5V6gQBmrTiMaKhlCG8Z6HzKR9vT0HIafNaojzBXhsoISduNUz11XwB96vqRaQkcC_KgTCFoKOwM6ClRw4ePUmQ0ZnF3Ib-2O-LUku3fWXrNMKsgQ\",\n" +
                "      \"e\": \"AQAB\",\n" +
                "      \"n\": \"v9-AW7Ox9PwnHqU_JMsmS84E3lqGEgzPKAXDT_Ma2YubJz1PDr61bq-yWt5boTgCWCii_bci1ym7rC5HnWxt8KDimzSy3wyIn39ASAXtUR6fUrZFnr8K78cP3fQf6-TedVxtMRYB1MTSR_-JOJ3vLTf-UaB75EMoAmmqZRkZFOHF6Z9KTEW8J6rYWD6CifVtGXLRB9edh9jTLfke8poArQOOgHH7ad5PNa9ksZslgd8tpNdoxagNYVGiIS5dY02qG3mWKAv2baTgDhXuk7N75o7GIzOQGaQIXd1SWQvNQV_RCarEMq4kwmfC2W2ZzUVb_bKgI69ywu6iZ0exJYWiZw\",\n" +
                "      \"kty\": \"RSA\",\n" +
                "      \"use\": \"sig\",\n" +
                "      \"kid\": \"lyric1\"\n" +

                "}");
    }

    private JsonObject getVendorKey(){
        return new JsonObject("{\n" +
                "            \"alg\": \"RS256\",\n" +
                "                \"e\": \"AQAB\",\n" +
                "                \"n\": \"gp4Kqosoo5GDhrWC180gyIug7KSee88M9SOSRTGSKgWrzk67g-88L5BoSo1faL2-8fYuEcbNVhc6XrT94lmxpMErKNMVnzBUPMZFqXMyzM71V3pfvgW9fyUO7TWTkR9i8I2OLp6HGwlOHJjeUJzXWdU6yp6FzB0gFi6PPrieiwSSnMbeAmXdYa2p90dpk8a8GzK0dGOWHntlPG8y7OZz1UvUJncT7B4HXFmdxlqNYj-O7dwZdvv0zbHp7hERzGn8DvvrAPNu8vyMYJrLEh0TFl9HRRa0q9xP6G7mcICv6LrTUaPjplRr2awPGC6rvzik9SWwYOor_oxnmVWmiKZsPQ\",\n" +
                "                \"kty\": \"RSA\",\n" +
                "                \"use\": \"enc\",\n" +
                "                \"kid\": \"vendor1\"\n" +
                "        }");
    }
}
