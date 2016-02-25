package com.lyric.test.integration;

import com.lyric.SecurityService;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.Key;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
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
            async.complete();
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

            String signedPayload = securityService.signPayload(assignment.toString(), "application/json").getCompactSerialization();
            String encryptedPayload = securityService.encryptPayload(signedPayload);

            request.end(encryptedPayload);
        }
        catch (JoseException e) {
            e.printStackTrace();
        }

    }

    private JsonObject getLyricKeys(){
        return new JsonObject("{\n" +

                "      \"alg\": \"RS256\",\n" +
                "      \"d\": \"YNjfnEFKYrPFWGndwqZmYoe6qTQ4pnZMcz-7xx4qMQj4Br7hNY-eBWLwhEd1XLfLwIht3aDmLWNF4_OuvR1teSoXx1gG5OQdUAFeP0HiGFS5amULppV1UfoZvnvm-E3OqnfBmtpuJ7kcHCeNkvsgFPt7Trtc-AuCU2ThCv5bsW5i_OMoFWSgdj98slKueBC4G_VVEe29w6rTm06mgJ-d7vB7-0HWF2tKkUGHfCPhqQZsOORMToC9XIPCcla-PpMOoaR2gJ9ZtXBduilHEf7JTZYipaUCIFEQdwiPOPvU897GJQUws8PCTyK11MRJiasqGrbL6AJnaMaO1i70v_fwAQ\",\n" +
                "      \"e\": \"AQAB\",\n" +
                "      \"n\": \"gLZomlXr62QMsFaqrR_xbWJU-m3Rn473aTiPkX6tg6vSOxf2ow6N-wVuA0fmRazQnUKGbWMyaQpmXv2a6qIHGDXpqdza8Bvrq8RvTr6NrOOvQhwwkbK67iNyFwmp4rQd3NCM-Kt9Gp4r97Z07JvCSOf_DmeOowN1nIcqnFJEXBd9_H1ZemP2paNMqXya9cPKYnogkG0E_jtK2ONPqYxQq5s-nOV3CFI8DFMPS43Y17PZGcNYRJrOSfTnTCE-TLQag9M_cvYadlqz8n2V_QyJEwqiIWfYwgej_NIbSUHrkG2qbvWg_VRwnwxo-XkEX9_wizBnsb0FnU8eaVU0pRp5rw\",\n" +
                "      \"kty\": \"RSA\",\n" +
                "      \"use\": \"sig\",\n" +
                "      \"kid\": \"lyric2\"\n" +

                "}");
    }

    private JsonObject getVendorKey(){
        return new JsonObject("{\n" +
                "            \"alg\": \"RS256\",\n" +
                "                \"e\": \"AQAB\",\n" +
                "                \"n\": \"wv9V0_9mHIAnpE3Lr6k6wRJ2QA7p4xhb_1AbbbP0NpPauZ9u_07AQw7Z6y36zLQ8donhDumkOSjqA5WuAoq1BEvs5eAHu0szRqE8_sxBobNaxgEQaw31FD250qkv8-9xpdJ5_wG4xJp0VCa37MJlqUjs2PbEmIkCLrosrdfwMxGMKDy7FRQePt95-R8Y-VMBg8VeUt6CFOCYkJeb6zri9uX4jMAUCRnOBlmtkIZ-SnidRYk1QOuvhy8P_IOn8h_vyUbBOyN-mvYct02WJsBHd3xQJE3klV2JUI3Wx0MxWAZHcGz6YV25G8w-d9XnKUVlWdNfO4BEOQubjRYNQ71YPQ\",\n" +
                "                \"kty\": \"RSA\",\n" +
                "                \"use\": \"enc\",\n" +
                "                \"kid\": \"demo-vendor\"\n" +
                "        }");
    }
}
