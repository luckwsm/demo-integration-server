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
import org.junit.Ignore;
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
            context.assertEquals("application/jose", resp.getHeader("content-type"));

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

    @Ignore
    @Test
    // TO RUN THIS TEST, SET DEFAULT_ASSIGNMENT_JOSE_FLAG TO FALSE IN THE BUILD.GRADLE FILE
    public void testShouldReturnResponse(TestContext context) {
        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        int random = getRandomNumber();

        final HttpClientRequest request = client.post(8080, "localhost", "/clients/" + random + "/assignments", resp -> {
            context.assertEquals(200, resp.statusCode());
            context.assertEquals("application/json", resp.getHeader("content-type"));

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


        request.end(assignment.toString());
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
                "        \"kty\":\"RSA\",\n" +
                "        \"kid\":\"demoAssignmentLyric\",\n" +
                "        \"n\":\"5TmciQXGpAHoyH-ZbN_ypExTaF4CKYYSr9Y8i9LICeY7lTXNbL2ZLwt1uCjKScBqSHMk76O0z2e1_3Gqkk1InjZKdmBHe0_80gan4DCq_a-Vgspqw7dwVvETO5hpI1tQ7lStoXwZElYfkBDp19kC2JKJl57DlHs1meAL1Y97hHbMHB3Mu1lnYYoX4M2N64X_CEvy09D9JyeS5hJ8PAmPvAtv9_GpnqYW4RcM7cb8qhaM9RlNTA8z3MYEppebqW2BoPr_QuY8YLMpMuvYcNORkkY4hEfvRQeJNvxJFw4zLodUlUXijM8djF2H87s_s9Iy3eteovFfPjpF1WrBCcGr_Q\",\n" +
                "        \"e\":\"AQAB\",\n" +
                "        \"d\":\"hxmXeuLempdNfa5G3PZ46gDaP-74U0TkCqK3-Kj4oW0CvQSC2I4-lI2qiinwX9t6SM0kfeD8lilICRATOjs2i5jv9ij0uGcCWT72_plwjihIdFec6VFacsOPVFaV0PLnFjDuTPj5RedgnuZ5DHCozsA_PA-ksgSUKOb10p7Ia98Cw7KAUjyyp9wexRswnc8IdBxmh2EA0Of011Xf5O3PmkxLYX5D9DAa9LGUr9hqBTC-K1qaDUPBjpFEDT7Y0KhSFe1kCBJPAYTzJ2O5mrypqkEtcyGzVjJH-o7cDw9TqCixtSCo4ImUuqdWEWbVFMAFPObgRNIaOkcd06wN6uBOQQ\",\n" +
                "        \"x5c\":[\"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\"]\n" +
                "        }");
    }

    private JsonObject getVendorKey(){
        return new JsonObject("{\n" +
                "            \"kty\":\"RSA\",\n" +
                "            \"kid\":\"demoAssignmentVendor\",\n" +
                "            \"n\":\"yGFKfMl5Igpzbrzwj28uputwFwhBuW8o4kS1oQhW5f2BPB2aM5L3Io1Bd9qtbqbwvnmnVXTn_wW987Li5CX1yeJdkOfl9fzk7DzaNydRoHTEkjSUFHoYvHLQPlyZLCYekeji5K1RfMTXu9u7c5iSs4JYi_80pS-RQZ5EqV_5yt2q2sjLKMg0zDBSugUiVpT-txrir8s4lQYeuXtGqMI3Y76m9h2xMId7M4Ewhv5DIP8O7EbwAxcF1Kmu-2T0ZrUcdvRsfu-8XnCM907goQcRRbXSl4K_iCxxt-BF0Dm6CEJZ2wVnaMIkDiWvgnkbiM3lIzvMYoGITjf2NVlabN0agw\",\n" +
                "            \"e\":\"AQAB\",\n" +
                "            \"x5c\":[\"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\"],\n" +
                "            \"d\":\"i_UggN58RCGGlM9VOuoObLo_lWbH-UxCAyC4uBxKDeQt8x4-CU3aXgDJvHw57PKfbnssPI0mScLp9SB6R56X-YWpkr_WqCUWhEbQp_tHkUpdzjz5PQA27NFI0HX-6o5JheJjMBQ6uWoHwQft-wSTWtxtGoOLcuExlOKF1MPbhixnSCW-XUByw9Ol83N3jQRHqJAQHPv3JoqWzU1y3rUr-ewnwIWzX8_1wxyVS5HdKjejBM1gSevjWH7oxbV5-BHj4pgU2xoywThth5AkW74-P8H3s5l10GvRhj6Hn183BVmEnAu8hZkREEm83VdkzUNMyDhUGdXJ3yI0m3FrT9uJgQ\",\n" +
                "            \"p\":\"2swtpQEerqzlu4laVs53IBhBVIBpOyW9daxHBLU28H1CWithVhxhiOqF5tsuwQPlzdi95y2rCvuIO79a6LvymqsJhQO3_wCMFcPQSQf6P23UlKdisTVMEssAKWd9wsfg85TVbrR3zN-j6n8W06kwDDEQQBFLTG26kOTwZzNyZ0E\",\n" +
                "            \"q\":\"6nNvNJEn6-qslExWPa3Sn5T4JJu14Lg30Ei5d1Th7hb6XpsoP44GeUuDWlBTjlPP2ENx1JYRciFHbYP1VR3n8xUkLksfDg3W3xmaTFhHdTan7TvnVuB4o9vm1BHaww2Hn_3wy2zHzPWR7mdFemvMqDK9x-koNZjNxg5vKjEvdMM\",\n" +
                "            \"dp\":\"BF7BpqMQFtMTP5yttkAndvelDmgGMg097ITFYl2wPDt0QNLO_eZROGfexs6n72kBhQwINv7dhLmjbfYTfcSLQvWZmhOJ8Ej1g1XG6j6DjBLIhTOWsA1teI6fcTRE-cf-QT8-yBhIsEnjP0LMlOuzLvsn5O-zflJOwKgh-R248kE\",\n" +
                "            \"dq\":\"Gr2t2m4QAp2Sr5uBXqmb023XW72S4h3A5_XTiaPZ3SErz2ydC5q0bFNLmRp-XUZ792vB6QBw3vaiwgACFIJDuXsY1FrnIOA9ONcfvUN4awsf3jQt77_MosvrKRccP5EkfgAl3d37cAo2ad3Hk-4Fus2Gze1vE6XpGoT88vAHPKc\",\n" +
                "            \"qi\":\"dx5845fvDZlebRk9TPbfnGgIsxYbZRcC8pLj-LCHX5EXIwQJaNuEFqFi5YdKKBxIMXJ5PfeHhAW4x5fVYYIGGIRGKC_x2Vh7fL5Mm-OKQhvwheEQblKG7qplqAUJHQttByY4OwnpmQoyhFyjcVH5d0wnniFHAYDslCn0rkZ1UnQ\"\n" +
                "         }");
    }
}
