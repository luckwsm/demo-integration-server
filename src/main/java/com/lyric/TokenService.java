package com.lyric;

import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;

import java.util.UUID;

/**
 * Created by amymadden on 7/8/16.
 */
public class TokenService {

    private final RsaJsonWebKey vendorRsaJsonWebKey;

    public TokenService(RsaJsonWebKey vendorRsaJsonWebKey) {
        this.vendorRsaJsonWebKey = vendorRsaJsonWebKey;
    }

    public String generateToken(String audience, String subject, String vendorId, boolean async) throws JoseException {

        // Create the Claims, which will be the content of the JWT
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(vendorId);  // who creates the token and signs it
        claims.setAudience(audience); // to whom the token is intended to be sent
        claims.setExpirationTimeMinutesInTheFuture(60); // time when the token will expire (10 minutes from now)
        claims.setJwtId(UUID.randomUUID().toString());; // a unique identifier for the token
        claims.setIssuedAtToNow();  // when the token was issued/created (now)
        claims.setNotBeforeMinutesInThePast(2); // time before which the token is not yet valid (2 minutes ago)
        claims.setSubject(subject); // the subject/principal is whom the token is about

        claims.setClaim("vendorId", vendorId); // additional claims/attributes about the subject can be added
        claims.setClaim("async", async);


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
        return jws.getCompactSerialization();
    }
}
