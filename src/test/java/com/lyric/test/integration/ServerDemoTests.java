package com.lyric.test.integration;

import com.google.common.io.Resources;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Created by amadden on 1/29/16.
 */
@RunWith(VertxUnitRunner.class)
public class ServerDemoTests extends TestsBase{

    @Test
    public void jsonCallWithCsvShouldReturnAccessToken(TestContext context){
        //System.setProperty("javax.net.debug", "ssl");

        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        int random = getRandomNumber();

        final HttpClientRequest request = client.post(8080, "localhost", "/clients/" + random + "/advance_server", resp -> {
            context.assertEquals(201, resp.statusCode());
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
    public void multipartCallWithCsvShouldReturnAccessToken(TestContext context){

        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        int random = getRandomNumber();
        //int random = 4532;

        final HttpClientRequest request = client.post(8080, "localhost", "/clients/" + random + "/advance_server", resp -> {
            context.assertEquals(201, resp.statusCode());
            String token = resp.getHeader("ACCESS_TOKEN");
            context.assertNotNull(token);
            async.complete();
        });
        request.headers().set("content-type", "application/json");

        JsonObject options = new JsonObject().put("options", new JsonObject()
                .put("contentType", "multipart/form-data")
                .put("royaltyEarningsContentType", "text/csv")
                .put("filename", "amytest.csv"));
        request.end(options.toString());
    }

    @Test
    public void testKeyStore(TestContext context) throws KeyStoreException {
        KeyStore ks = KeyStore.getInstance("pkcs12");
        URL resource = Resources.getResource("dev/certificate.pfx");
        try {
            ks.load(resource.openStream(), "lyric_changeme".toCharArray());
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            e.printStackTrace();
        }
    }
}
