package com.lyric;

import com.google.common.collect.Sets;
import com.lyric.controllers.TokenController;
import com.lyric.controllers.AssignmentsController;
import com.lyric.controllers.ClientDemoController;
import com.lyric.controllers.ServerDemoController;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import org.apache.commons.lang3.EnumUtils;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.lang.JoseException;

import java.util.Arrays;
import java.util.Set;

/**
 * Created by amadden on 1/29/16.
 */
public class ApiServer extends AbstractVerticle {
    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";

    Logger logger = LoggerFactory.getLogger(ApiServer.class.getName());
    private static Set<HttpMethod> allHttpMethods = Sets.newConcurrentHashSet(EnumUtils.getEnumList(HttpMethod.class));
    private static Set<String> allowedCorsHeaders = Sets.newConcurrentHashSet(Arrays.asList("Accept", "Authorization", "Content-Type", "vendorId"));
    private CorsHandler corsHandler = CorsHandler.create("*").allowedMethods(allHttpMethods).allowedHeaders(allowedCorsHeaders).exposedHeader(ACCESS_TOKEN).exposedHeader("TOKEN");

    @Override
    public void start(Future<Void> startFuture) throws JoseException {
        final String assignmentsApiJson = ConfigFactory.parseResourcesAnySyntax("specs/v1/assignments-api").root().render(ConfigRenderOptions.concise().setFormatted(true));

        io.vertx.ext.web.Router router = io.vertx.ext.web.Router.router(vertx);

        router.route().handler(corsHandler);
        router.route().handler(CorsHandler.create("*"));

        router.route().handler(BodyHandler.create());

        final JsonObject lyricJsonKey = config().getJsonObject("lyric").getJsonObject("key");
        final RsaJsonWebKey lyricRsaJsonWebKey = (RsaJsonWebKey) JsonWebKey.Factory.newJwk(lyricJsonKey.toString());

        final JsonObject localJsonKey = config().getJsonObject("local").getJsonObject("key");
        final RsaJsonWebKey localRsaJsonWebKey = (RsaJsonWebKey) JsonWebKey.Factory.newJwk(localJsonKey.toString());

        final ClientDemoController clientDemoController = new ClientDemoController(vertx);
        final ServerDemoController serverDemoController = new ServerDemoController(vertx);
        final AssignmentsController assignmentsController = new AssignmentsController(vertx, lyricRsaJsonWebKey, localRsaJsonWebKey);
        final TokenController tokenController = new TokenController(vertx, lyricRsaJsonWebKey, localRsaJsonWebKey);

        router.post("/clients/:id/advance_client").handler(clientDemoController::create);
        router.post("/clients/:id/advance_server").handler(serverDemoController::create);

        router.get("/token").handler(tokenController::getToken);

        router.post("/clients/:id/assignments").handler(assignmentsController::create);

        router.route("/specs/v1/assignments-api.json").handler(routingContext -> {
            routingContext.response().end(assignmentsApiJson);
        });

        router.get("/docs/assignments-api/*").handler(StaticHandler.create("assignments-api"));

        final JsonObject serverOptions = new JsonObject();

        serverOptions.put("port", Integer.parseInt(System.getenv("PORT"))).put("host", "0.0.0.0");

        vertx.createHttpServer(new HttpServerOptions(serverOptions)).requestHandler(router::accept).listen(asyncResult -> {
            if (asyncResult.failed()) {
                startFuture.fail(asyncResult.cause());
                return;
            }
            logger.info("Http Server Started Successfully");
            startFuture.complete();
        });
    }

}
