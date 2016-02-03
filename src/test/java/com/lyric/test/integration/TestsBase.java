package com.lyric.test.integration;

import com.lyric.ApiServer;
import com.lyric.Start;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.Random;

/**
 * Created by amadden on 1/29/16.
 */

public class TestsBase {
    Vertx vertx;

    @Before
    public void setUp(TestContext context) throws Exception {
        vertx = Vertx.vertx();

        DeploymentOptions options = new DeploymentOptions();
        vertx.deployVerticle(Start.class.getName(), options, context.asyncAssertSuccess(resp -> {
            System.out.println("SUCCESS");
        }));
    }

    @After
    public void tearDown(TestContext context) throws Exception {
        vertx.close(context.asyncAssertSuccess());
    }

    protected int getRandomNumber() {
        int START = 1000;
        int END = 9999;
        Random r = new Random();
        return r.nextInt((END - START) + 1) + START;
    }
}
