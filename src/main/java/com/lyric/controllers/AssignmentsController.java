package com.lyric.controllers;

import com.lyric.AssignmentService;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Created by amadden on 1/29/16.
 */
public class AssignmentsController {
    private final Vertx vertx;
    private final RsaJsonWebKey lyricRsaJsonWebKey;
    private final RsaJsonWebKey vendorRsaJsonWebKey;

    private final AssignmentService assignmentService;

    Logger logger = LoggerFactory.getLogger(AssignmentsController.class.getName());

    public AssignmentsController(Vertx vertx, RsaJsonWebKey lyricRsaJsonWebKey, RsaJsonWebKey vendorRsaJsonWebKey) {
        this.vertx = vertx;
        this.lyricRsaJsonWebKey = lyricRsaJsonWebKey;
        this.vendorRsaJsonWebKey = vendorRsaJsonWebKey;

        assignmentService = new AssignmentService();
    }

    public void create(RoutingContext routingContext){

        final HttpServerResponse response = routingContext.response();
        try {
            String signedPayload = decryptPayload(routingContext.getBodyAsString(), vendorRsaJsonWebKey.getRsaPrivateKey());


            JsonWebSignature jws = getJws(signedPayload, lyricRsaJsonWebKey.getRsaPublicKey());

            if(!jws.verifySignature()){
                logger.error("Signature is not Verified");
                response.setStatusCode(500);
                response.end();
                return;
            }

            assignmentService.assign(new JsonObject(jws.getPayload()));

            JsonObject responseObject = new JsonObject().put("test", "test");

            JsonWebSignature responseJws = signPayload(vendorRsaJsonWebKey.getRsaPrivateKey(), vendorRsaJsonWebKey.getKeyId(), responseObject.toString());
            String compactSerialization = encryptPayload(responseJws.getCompactSerialization(), lyricRsaJsonWebKey.getRsaPublicKey());


            response.end(responseJws.getCompactSerialization());
        } catch (JoseException e) {
            logger.error(e);
            response.setStatusCode(500);
            response.setStatusMessage(e.getMessage());
            response.end();
        }
    }

    private String decryptPayload(String payload, RSAPrivateKey vendorPrivateKey) throws JoseException {
        JsonWebEncryption jwe = new JsonWebEncryption();

        // Set the compact serialization on new Json Web Encryption object, which is the payload of
        // the verified jsonWebSignature
        jwe.setCompactSerialization(payload);

        // Symmetric encryption, like we are doing here, requires that both parties have the same key.
        // The key will have had to have been securely exchanged out-of-band somehow.
        jwe.setKey(vendorPrivateKey);

        // Get the message that was encrypted in the JWE. This step performs the actual decryption steps.
        String decryptedPayload = jwe.getPlaintextString();
        return decryptedPayload;
    }

    private JsonWebSignature getJws(String body, RSAPublicKey lyricsPublicKey) throws JoseException {

        JsonWebSignature jws = new JsonWebSignature();

        // Set the compact serialization on the JWS to the body of the request
        jws.setCompactSerialization(body);
        jws.setKey(lyricsPublicKey);
        return jws;
    }

    private JsonWebSignature signPayload(RSAPrivateKey vendorPrivateKey, String encryptedResponse, String keyId) {

        JsonWebSignature jws = new JsonWebSignature();

        // The payload of the JWS is compactSerialization of the JWE
        jws.setPayload(encryptedResponse);

        // The JWT is signed using the private key
        jws.setKey(vendorPrivateKey);

        jws.setKeyIdHeaderValue(keyId);

        // Set the signature algorithm on the JWS that will integrity protect the response
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        return jws;
    }

    private String encryptPayload(String signedAssignment, RSAPublicKey lyricPublicKey) throws JoseException {
        // Create a new Json Web Encryption object
        JsonWebEncryption senderJwe = new JsonWebEncryption();

        // The plaintext of the JWE is the message that we want to encrypt.
        senderJwe.setPlaintext(signedAssignment);

        // Set the "alg" header, which indicates the key management mode for this JWE.
        senderJwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.RSA1_5);

        // Set the "enc" header, which indicates the content encryption algorithm to be used.
        senderJwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);

        // Set the key on the JWE.
        senderJwe.setKey(lyricPublicKey);

        // Produce the JWE compact serialization, which is where the actual encryption is done.
        // The JWE compact serialization consists of five base64url encoded parts
        // combined with a dot ('.') character in the general format of
        // <header>.<encrypted key>.<initialization vector>.<ciphertext>.<authentication tag>
        // Direct encryption doesn't use an encrypted key so that field will be an empty string
        // in this case.
        return senderJwe.getCompactSerialization();
    }
}
