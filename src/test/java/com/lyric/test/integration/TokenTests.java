package com.lyric.test.integration;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by amymadden on 4/5/16.
 */
@RunWith(VertxUnitRunner.class)
public class TokenTests extends TestsBase{
    Logger logger = LoggerFactory.getLogger(TokenTests.class.getName());

    @Test
    public void testShouldReturnToken(TestContext context) {
        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        final HttpClientRequest request = client.get(8080, "localhost", "/token?vendorClientAccountId=amySonyTest", resp -> {

            resp.bodyHandler(buffer -> {
                logger.info(resp.getHeader("TOKEN"));
                context.assertEquals(200, resp.statusCode());
                async.complete();
            });
        });

        request.end();
    }
}
