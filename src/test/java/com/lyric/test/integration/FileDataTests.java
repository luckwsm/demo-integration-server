package com.lyric.test.integration;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by amymadden on 2/11/17.
 */
@RunWith(VertxUnitRunner.class)
public class FileDataTests extends TestsBase {

    Logger logger = LoggerFactory.getLogger(FileDataTests.class.getName());

    @Test
    public void testShouldGetTunecoreDistributionSampleFileData(TestContext context){

        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        int random = getRandomNumber();
        final int numberOfPeriods = 14;
        final int recordsPerPeriod = 6;
        final int totalExpectedRows = numberOfPeriods * recordsPerPeriod;

        final HttpClientRequest request = client.post(8080, "localhost", "/clients/" + random + "/getfiledata", resp -> {
            context.assertEquals(200, resp.statusCode());

            resp.bodyHandler(data -> {
                logger.info(data);
                JsonArray rows = new JsonArray(data.toString());
                context.assertEquals(numberOfPeriods, rows.size());
                async.complete();
            });

        });
        request.headers().set("content-type", "application/json");

        JsonObject fileOptions = new JsonObject()
                .put("vendorType", "distributor")
                .put("frequencyInDays", 30)
                .put("numberOfPeriods", numberOfPeriods)
                .put("numberOfRecordsPerPeriod", recordsPerPeriod)
                .put("schemas", new JsonArray().add("TunecoreDistributionSample"));

        JsonObject data = new JsonObject().put("fileOptions", fileOptions);

        request.end(data.toString());
    }

    @Test
    public void testShouldGetSonyAtvStatementSummaryFileData(TestContext context){

        HttpClient client = vertx.createHttpClient();
        Async async = context.async();

        int random = getRandomNumber();
        final int numberOfPeriods = 4;
        final int totalExpectedRows = numberOfPeriods;

        final HttpClientRequest request = client.post(8080, "localhost", "/clients/" + random + "/getfiledata", resp -> {
            context.assertEquals(200, resp.statusCode());

            resp.bodyHandler(data -> {
                logger.info(data);
                JsonArray rows = new JsonArray(data.toString());
                context.assertEquals(totalExpectedRows, rows.size());
                async.complete();
            });

        });
        request.headers().set("content-type", "application/json");

        JsonObject fileOptions = new JsonObject()
                .put("vendorType", "publisher")
                .put("frequencyInDays", 182)
                .put("numberOfPeriods", numberOfPeriods)
                .put("schemas", new JsonArray().add("SonyatvStatementSummary"));

        JsonObject data = new JsonObject().put("fileOptions", fileOptions);

        request.end(data.toString());
    }

}
