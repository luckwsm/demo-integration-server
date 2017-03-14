package com.lyric;

import com.google.common.collect.Sets;
import com.lyric.controllers.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.apache.commons.lang3.EnumUtils;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.lang.JoseException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by amadden on 1/29/16.
 */
public class ApiServer extends AbstractVerticle {
    public static final String ACCESS_TOKEN = "access-token";

    Logger logger = LoggerFactory.getLogger(ApiServer.class.getName());
    private static Set<HttpMethod> allHttpMethods = Sets.newConcurrentHashSet(EnumUtils.getEnumList(HttpMethod.class));
    private static Set<String> allowedCorsHeaders = Sets.newConcurrentHashSet(Arrays.asList("Accept", "Authorization", "Content-Type", "vendor-id", "async-token"));
    private CorsHandler corsHandler = CorsHandler.create("*").allowedMethods(allHttpMethods).allowedHeaders(allowedCorsHeaders).exposedHeader(ACCESS_TOKEN).exposedHeader("TOKEN");

    @Override
    public void start(Future<Void> startFuture) throws JoseException {

        io.vertx.ext.web.Router router = io.vertx.ext.web.Router.router(vertx);

        router.route().handler(corsHandler);
        router.route().handler(CorsHandler.create("*"));

        router.route().handler(BodyHandler.create());

        Map<String, RsaJsonWebKey> lyricAssignmentRsaJsonWebKeyMap = new HashMap<>();
        Map<String, RsaJsonWebKey> localAssignmentRsaJsonWebKeyMap = new HashMap<>();

        Map<String, RsaJsonWebKey> lyricApiRsaJsonWebKeyMap = new HashMap<>();
        Map<String, RsaJsonWebKey> localApiRsaJsonWebKeyMap = new HashMap<>();

        for (String fieldName : config().getJsonObject("keys").fieldNames()) {
            JsonObject vendorConfig = config().getJsonObject("keys").getJsonObject(fieldName);

            final JsonObject lyricAssignmentJsonKey = vendorConfig.getJsonObject("lyricAssignment").getJsonObject("key");
            final RsaJsonWebKey lyricAssignmentRsaJsonWebKey = (RsaJsonWebKey) JsonWebKey.Factory.newJwk(lyricAssignmentJsonKey.toString());
            lyricAssignmentRsaJsonWebKeyMap.put(fieldName, lyricAssignmentRsaJsonWebKey);

            final JsonObject lyricApiJsonKey = vendorConfig.getJsonObject("lyricApi").getJsonObject("key");
            final RsaJsonWebKey lyricApiRsaJsonWebKey = (RsaJsonWebKey) JsonWebKey.Factory.newJwk(lyricApiJsonKey.toString());
            lyricApiRsaJsonWebKeyMap.put(fieldName, lyricApiRsaJsonWebKey);

            final JsonObject localAssignmentJsonKey = vendorConfig.getJsonObject("localAssignment").getJsonObject("key");
            final RsaJsonWebKey localAssignmentRsaJsonWebKey = (RsaJsonWebKey) JsonWebKey.Factory.newJwk(localAssignmentJsonKey.toString());
            localAssignmentRsaJsonWebKeyMap.put(fieldName, localAssignmentRsaJsonWebKey);

            final JsonObject localApiJsonKey = vendorConfig.getJsonObject("localApi").getJsonObject("key");
            final RsaJsonWebKey localApiRsaJsonWebKey = (RsaJsonWebKey) JsonWebKey.Factory.newJwk(localApiJsonKey.toString());
            localApiRsaJsonWebKeyMap.put(fieldName, localApiRsaJsonWebKey);
        }

        final SecurityService assignmentSecurityService = new SecurityService(lyricAssignmentRsaJsonWebKeyMap, localAssignmentRsaJsonWebKeyMap);
        final SecurityService apiSecurityService = new SecurityService(lyricApiRsaJsonWebKeyMap, localApiRsaJsonWebKeyMap);


        final TokenService tokenService = new TokenService(localApiRsaJsonWebKeyMap);

        final MultiCallDemoController multiCallDemoController = new MultiCallDemoController(vertx, apiSecurityService);
        final LyricDemoController lyricDemoController = new LyricDemoController(vertx, apiSecurityService);
        final AssignmentsController assignmentsController = new AssignmentsController(vertx, assignmentSecurityService);
        final TokenController tokenController = new TokenController(vertx, tokenService);
        final FileDataController fileDataController = new FileDataController(vertx);

        router.post("/clients/:id/advance_multi").handler(multiCallDemoController::create);

        router.post("/clients/:id/advance").handler(lyricDemoController::create);

        router.get("/token").handler(tokenController::getToken);
        router.get("/asynctoken").handler(tokenController::getAsyncToken);

        router.post("/clients/:id/assignments").handler(assignmentsController::create);
        router.get("/clients/:id/assignments").handler(assignmentsController::get);

        router.post("/clients/:id/getfiledata").handler(fileDataController::getFileData);


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
