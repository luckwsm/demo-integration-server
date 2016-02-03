package com.lyric;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

/**
 * Created by amadden on 2/1/16.
 */
public class Start extends AbstractVerticle {

    public void start(final Future<Void> startedResult) {

        final DeploymentOptions options = new DeploymentOptions();
        options.setConfig(new JsonObject(ConfigFactory.load().root().render(ConfigRenderOptions.concise())));

        vertx.deployVerticle(ApiServer.class.getName(), options, deployId -> {
            if (deployId.failed()) {
                final Throwable cause = deployId.cause();
                //logger.error(cause.getMessage(), cause);
                startedResult.fail(deployId.cause());
                return;
            }

            startedResult.complete();

        });
    }
}
