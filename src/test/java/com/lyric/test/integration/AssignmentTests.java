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
                "      \"d\": \"UnuQUyXS1Wm76PjQxtlMKv06kfisjcS3Pu_wuSooXyjkY0yVuOUUmQ2bkV1GnZNpN-QSsrmfC5hQc_786ynk2oD5Jn9oYLDBaJgbHr55jz5jBMm4HmP_1gG-17G0JQ9Jr52Uy3u69DBKtL36KSeDzEcvyFrhYsz-W0jAw4Mhottl9HOa5pSEPeF_mzjQ71Oot0L0XiXbiOklLEm7zKumtdARZFpM_yW_YAPEyms9u8Q4MGq415aHPJF_ec8cqaVCyYgR1b9OMfKiOntkpFURLWCD_JYn-pkFLdVJeabKhLWhQ66fpDVI6VwCpj7x-UmbC_7OrOLlB2o7VYh_bafRUQ\",\n" +
                "      \"e\": \"AQAB\",\n" +
                "      \"n\": \"kG-mk5kcMJ-vlAMvkG_6_rW9exOmBwpV6PvvQ0kvxFzG4syVJy-apLLqZEhXaNGhDcHvtlHnDi6fdcUnSns0hYFRWnHJCHnS8jv4_ODMGEu_8Wv8K5YaJKWQreh3_tHR_82vwisjFujN5WH8FNw7gg1MNcEqGxpztcqzpuK34JNXoafd23wqaV6-oa26HBQEjBvk-pXs8ip0qQYAc9aDAfOdYSHYrRSXku68zU5nqdKwDbmayIpopiDz4ugnPWH3XnyQy3JuHHIzTHR8ptHiI2I3NgxOQk64PJ3Hx2FMJFY535MRaI-uySqCu2tOzB7PMgvQaf5LA36zkeVka62EQw\",\n" +
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
