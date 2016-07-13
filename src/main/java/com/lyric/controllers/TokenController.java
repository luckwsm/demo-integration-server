package com.lyric.controllers;

import com.lyric.ClientRepository;
import com.lyric.TokenService;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;

/**
 * Created by amadden on 2/3/16.
 */
public class TokenController {
    private final Vertx vertx;
    private final TokenService tokenService;
    Logger logger = LoggerFactory.getLogger(TokenController.class.getName());

    public TokenController(Vertx vertx, TokenService tokenService) {
        this.vertx = vertx;
        this.tokenService = tokenService;
    }

    public void getToken(RoutingContext routingContext) {
        String defaultVendorId = System.getenv("DEFAULT_VENDOR_ID");
        String vendorClientAccountId = routingContext.request().getParam("vendorClientAccountId");

        handleGetToken(routingContext, "widgetApi", vendorClientAccountId, defaultVendorId, false);
    }

    public void getAsyncToken(RoutingContext routingContext) {
        String defaultVendorId = System.getenv("DEFAULT_VENDOR_ID");
        String vendorClientAccountId = routingContext.request().getParam("vendorClientAccountId");

        handleGetToken(routingContext, "vatmApi", vendorClientAccountId, defaultVendorId, true);
    }

    private void handleGetToken(RoutingContext routingContext, String audience, String subject, String vendorId, boolean async) {
        final HttpServerResponse response = routingContext.response();
        String jwt = null;
        try {
            jwt = tokenService.generateToken(audience, subject, vendorId, async);
        } catch (JoseException e) {
            logger.error(e.getCause());
            response.setStatusCode(500);
            response.setStatusMessage(e.getMessage());
            response.end();
        }

        response.putHeader("TOKEN", jwt);
        response.end();
    }
}
