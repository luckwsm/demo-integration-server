package com.lyric.controllers;

import com.lyric.ClientRepository;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerRequest;
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
public class AdvanceStatusController {
    private final Vertx vertx;
    private final RsaJsonWebKey lyricRsaJsonWebKey;
    private final RsaJsonWebKey vendorRsaJsonWebKey;
    Logger logger = LoggerFactory.getLogger(AdvanceStatusController.class.getName());

    public AdvanceStatusController(Vertx vertx, RsaJsonWebKey lyricRsaJsonWebKey, RsaJsonWebKey vendorRsaJsonWebKey) {
        this.vertx = vertx;
        this.lyricRsaJsonWebKey = lyricRsaJsonWebKey;
        this.vendorRsaJsonWebKey = vendorRsaJsonWebKey;
    }

    public void getAdvanceStatus(RoutingContext routingContext){
        JsonObject status = new JsonObject()
                .put("advanceAmount", 5000.00)
                .put("amountRepaid", 3545.21)
                .put("amountRemaining", 1454.79)
                .put("availableBalance", 3545.21);

        routingContext.response().putHeader("content-type", "application/json").end(status.encodePrettily());
    }

    public void getAdvanceToken(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");

        JsonObject client = ClientRepository.findClient(id, true);

        // Create the Claims, which will be the content of the JWT
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("DemoVendor");  // who creates the token and signs it
        claims.setAudience("Lyric"); // to whom the token is intended to be sent
        claims.setExpirationTimeMinutesInTheFuture(10); // time when the token will expire (10 minutes from now)
        claims.setGeneratedJwtId(); // a unique identifier for the token
        claims.setIssuedAtToNow();  // when the token was issued/created (now)
        claims.setNotBeforeMinutesInThePast(2); // time before which the token is not yet valid (2 minutes ago)
        claims.setSubject(client.getJsonObject("vendorAccount").getString("memberToken")); // the subject/principal is whom the token is about
        //claims.setClaim("memberToken", client.getJsonObject("vendorAccount").getString("memberToken")); // additional claims/attributes about the subject can be added

        // A JWT is a JWS and/or a JWE with JSON claims as the payload.
        // In this example it is a JWS so we create a JsonWebSignature object.
        JsonWebSignature jws = new JsonWebSignature();

        // The payload of the JWS is JSON content of the JWT Claims
        jws.setPayload(claims.toJson());

        // The JWT is signed using the private key
        jws.setKey(vendorRsaJsonWebKey.getPrivateKey());

        // Set the Key ID (kid) header because it's just the polite thing to do.
        // We only have one key in this example but a using a Key ID helps
        // facilitate a smooth key rollover process
        jws.setKeyIdHeaderValue(vendorRsaJsonWebKey.getKeyId());

        // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

        // Sign the JWS and produce the compact serialization or the complete JWT/JWS
        // representation, which is a string consisting of three dot ('.') separated
        // base64url-encoded parts in the form Header.Payload.Signature
        // If you wanted to encrypt it, you can simply set this jwt as the payload
        // of a JsonWebEncryption object and set the cty (Content Type) header to "jwt".
        String jwt = null;
        final HttpServerResponse response = routingContext.response();
        try {
            jwt = jws.getCompactSerialization();
        } catch (JoseException e) {
            logger.error(e.getCause());
            response.setStatusCode(500);
            response.setStatusMessage(e.getMessage());
            response.end();
        }
        logger.info("ABOUT TO SET TOKEN IN HEADER: " + jwt);
        response.putHeader("TOKEN", jwt);
        response.end();
    }
}
