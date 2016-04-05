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
        return new JsonObject("{\"kty\":\"RSA\"," +
                "\"kid\":\"demoAssignmentLyric\"," +
                "\"n\":\"1b-YbDM3fKZjdHlrS59X7BxZwEqeh-NiSoMc_yUB9XzNywtrVYeCj5SXaltJA0neYsqeaZaMl767Za7VYIajjredrxn4BFDeJYnDZgEJh_3DOtrOjBUU2ydqenqiHGYkO5DN4LstFilhYcVG4VkgYY_GELQ7KmOh8S83XxrDmB_AXZBlRcuj4UVAxS7beeDgcKAvSVA0ZkOlKEM_Oi39o2h9VHkt2qPvi43EuIydooPKgsMdvgBx9pgbUA2Q5Akj5QehVvI0WfXxtAkZuFCU6cNN7WNwvfnGX0wl3F74vGS9RLL-mxg7aChLJxi4ZK-jB9u7gH9ug3sOw-T-361FoQ\"," +
                "\"e\":\"AQAB\"," +
                "\"d\":\"YnHj2bGdFKPtmm9xo1q4O96y14WGjj-DvZ21dS2uQUpB3gpcXQAUedyOAEYa0pDrA8UeQLM9_YWy-ZoUXGimW8zlqwfi2toD9nYEsK45nZCaoji0eIAdIvJ8b_ybPUkt1uFScPbERTi5yTX69z79UvHM31SjC1rn8xU0A3FoNF9Th7L6PLVw41-wk8Uw60g8e2mn1-LmYM3PN3TuFkcyaqbznmvl6hkdcf9LfCwBbRBCdU83l4GgZ7Re8H2Ra6Ov0RqumUZWgga-HhPp6CXkmYqqyOeoxv2dV59_lpWxhP6qaSUDIWOfUUw1Q3nMzh2J2FV3J-Ghm5EYbazOyg2LgQ\"" +
                "}");
    }

    private JsonObject getVendorKey(){
        return new JsonObject("{\n" +
                "           \"kty\":\"RSA\",\n" +
                "           \"kid\":\"demoAssignmentVendor\",\n" +
                "           \"n\":\"i9WApEijuYQYQNq4TyAXFjgBM8-5fI8rFEPhnU4o_oq6a3Eb_KpiourGZ1IExNDFGKlV-gcJE-8rsft3_EvNGH0faiwAxMkI-c3EmLPokBcGDr_7ekKRBNLhDmlgP01WIj38e5UjjqJliZU3-vTg8NeBqMLbVPtKBVrczSZORv3wCbGi6sm6F1oXcenywGLBIoKHrGS9EBU7DZJ3mVljQ8ewkomx_cX8nzn4RRx9mD60LBpi9oWlo5Tr9s_1t_mCrdx4cC1E_HS_ddGiH5Tzpoe4sAYnP-tasvhMKeuNV5hwgsn8T8typiJ27cGcoWlWOA9pXjDED0waunmYFdLrmQ\",\n" +
                "           \"e\":\"AQAB\",\n" +
                "           \"d\":\"irJeQpT6MExhchJ_tZvyyksKhkWHiZDH_fXGlqyr5K3dcixiwh6Ob2HHITwtFrqshGWRrcnEq00TXho7TLgy2hNhUYIL0SRGDuCWG5B3IFR14bNy0DWuecZhDFZg7OjcgmJRSJMJDbTuWaT0PaseYkSfirCbtJL7PIx_bcyM38QQ6xfwRCj3Zvj2EH6FxLfEMCp5OgPc4BoGaoGc-wVkNtxkK64KsJmoAkW-XORbHNCTn_Q6v4MPTpucWqn6wtZdXGoSoxbmQYGW8iDcLyyQ-H4u1bYv0Gxbt_MuvBBXCkPdTZaUyzzdvQxArPKKpSSapRE0I9pirFPR6D1_K-SvwQ\"\n" +
                "         }");
    }
}
