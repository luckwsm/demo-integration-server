package com.lyric;

import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;

/**
 * Created by amymadden on 2/24/16.
 */
public class SecurityService {
    private final RsaJsonWebKey remoteRsaJsonWebKey;
    private final RsaJsonWebKey localRsaJsonWebKey;

    public SecurityService(RsaJsonWebKey remoteRsaJsonWebKey, RsaJsonWebKey localRsaJsonWebKey) {
        this.remoteRsaJsonWebKey = remoteRsaJsonWebKey;
        this.localRsaJsonWebKey = localRsaJsonWebKey;
    }

    public String decryptPayload(String payload) throws JoseException {
        JsonWebEncryption jwe = new JsonWebEncryption();

        // Set the compact serialization on new Json Web Encryption object, which is the payload of
        // the verified jsonWebSignature
        jwe.setCompactSerialization(payload);

        // Symmetric encryption, like we are doing here, requires that both parties have the same key.
        // The key will have had to have been securely exchanged out-of-band somehow.
        jwe.setKey(localRsaJsonWebKey.getRsaPrivateKey());

        // Get the message that was encrypted in the JWE. This step performs the actual decryption steps.
        String decryptedPayload = jwe.getPlaintextString();
        return decryptedPayload;
    }

    public String encryptPayload(String payload) throws JoseException {
        // Create a new Json Web Encryption object
        JsonWebEncryption senderJwe = new JsonWebEncryption();

        // The plaintext of the JWE is the message that we want to encrypt.
        senderJwe.setPlaintext(payload);

        // Set the "alg" header, which indicates the key management mode for this JWE.
        senderJwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.RSA1_5);

        // Set the "enc" header, which indicates the content encryption algorithm to be used.
        senderJwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);

        // Set the key on the JWE.
        senderJwe.setKey(remoteRsaJsonWebKey.getRsaPublicKey());

        // Produce the JWE compact serialization, which is where the actual encryption is done.
        // The JWE compact serialization consists of five base64url encoded parts
        // combined with a dot ('.') character in the general format of
        // <header>.<encrypted key>.<initialization vector>.<ciphertext>.<authentication tag>
        // Direct encryption doesn't use an encrypted key so that field will be an empty string
        // in this case.
        return senderJwe.getCompactSerialization();
    }

    public JsonWebSignature signPayload(String payload, String cty) {
        JsonWebSignature jws = new JsonWebSignature();

        // The payload of the JWS is compactSerialization of the JWE
        jws.setPayload(payload);

        // The JWT is signed using the private key
        jws.setKey(localRsaJsonWebKey.getRsaPrivateKey());

        jws.setContentTypeHeaderValue(cty);

        jws.setKeyIdHeaderValue(localRsaJsonWebKey.getKeyId());

        // Set the signature algorithm on the JWS that will integrity protect the response
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        return jws;
    }

    public JsonWebSignature getJws(String body) throws JoseException {

        JsonWebSignature jws = new JsonWebSignature();

        // Set the compact serialization on the JWS to the body of the request
        jws.setCompactSerialization(body);
        jws.setKey(remoteRsaJsonWebKey.getRsaPublicKey());
        return jws;
    }
}
