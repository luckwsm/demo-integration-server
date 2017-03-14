package com.lyric;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import io.vertx.core.json.JsonArray;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by amymadden on 2/24/16.
 */
public class SecurityService {
    private HashFunction hashFunction = Hashing.sha256();
    public static final String HEADER_SIGNATURE = "SIGNATURE";

    private final Map<String, RsaJsonWebKey> remoteRsaJsonWebKeyMap;
    private final Map<String, RsaJsonWebKey> localRsaJsonWebKeyMap;

    public SecurityService(Map<String, RsaJsonWebKey> remoteRsaJsonWebKeyMap, Map<String, RsaJsonWebKey> localRsaJsonWebKeyMap) {
        this.remoteRsaJsonWebKeyMap = remoteRsaJsonWebKeyMap;
        this.localRsaJsonWebKeyMap = localRsaJsonWebKeyMap;
    }

    public JsonWebSignature createSignature(byte[] payload, String vendorId) {
        RsaJsonWebKey localRsaJsonWebKey = localRsaJsonWebKeyMap.get(vendorId);

        final HashCode contentHash = hashFunction.hashBytes(payload);

        JsonWebSignature jws = new JsonWebSignature();

        // The payload of the JWS is compactSerialization of the JWE
        jws.setPayload(contentHash.toString());

        // The JWT is signed using the private key
        jws.setKey(localRsaJsonWebKey.getRsaPrivateKey());

        jws.setContentTypeHeaderValue("text/plain");

        jws.setKeyIdHeaderValue(localRsaJsonWebKey.getKeyId());

        // Set the signature algorithm on the JWS that will integrity protect the response
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        return jws;
    }

    public String encryptPayload(JsonWebSignature signature, byte[] payload, String cty, JsonArray additionalJweHeaders, String vendorId) throws JoseException {
        RsaJsonWebKey remoteRsaJsonWebKey = remoteRsaJsonWebKeyMap.get(vendorId);
        // Create a new Json Web Encryption object
        JsonWebEncryption jwe = new JsonWebEncryption();

        jwe.setHeader(HEADER_SIGNATURE, signature.getCompactSerialization());
        jwe.setContentTypeHeaderValue(cty);

        if(additionalJweHeaders != null && !additionalJweHeaders.isEmpty()){
            for (Object additionalJweHeader : additionalJweHeaders) {
                String[] parts = additionalJweHeader.toString().split("\\|");
                jwe.setHeader(parts[0], parts[1]);
            }
        }

        jwe.enableDefaultCompression();

        // The plaintext of the JWE is the message that we want to encrypt.
        jwe.setPlaintext(payload);

        // Set the "alg" header, which indicates the key management mode for this JWE.
        jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.RSA1_5);

        // Set the "enc" header, which indicates the content encryption algorithm to be used.
        jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);

        // Set the key on the JWE.
        jwe.setKey(remoteRsaJsonWebKey.getRsaPublicKey());
        jwe.setKeyIdHeaderValue(remoteRsaJsonWebKey.getKeyId());

        // Produce the JWE compact serialization, which is where the actual encryption is done.
        // The JWE compact serialization consists of five base64url encoded parts
        // combined with a dot ('.') character in the general format of
        // <header>.<encrypted key>.<initialization vector>.<ciphertext>.<authentication tag>
        // Direct encryption doesn't use an encrypted key so that field will be an empty string
        // in this case.
        return jwe.getCompactSerialization();
    }

    public JsonWebEncryption decryptPayload(String payload, String vendorId) throws JoseException {
        RsaJsonWebKey localRsaJsonWebKey = localRsaJsonWebKeyMap.get(vendorId);
        JsonWebEncryption jwe = new JsonWebEncryption();

        // Set the compact serialization on new Json Web Encryption object, which is the payload of
        // the verified jsonWebSignature
        jwe.setCompactSerialization(payload);

        // Symmetric encryption, like we are doing here, requires that both parties have the same key.
        // The key will have had to have been securely exchanged out-of-band somehow.
        jwe.setKey(localRsaJsonWebKey.getRsaPrivateKey());

        return jwe;
    }

    public boolean isValidSignature(JsonWebEncryption jwe, String vendorId) throws JoseException {
        RsaJsonWebKey remoteRsaJsonWebKey = remoteRsaJsonWebKeyMap.get(vendorId);
        String signature = jwe.getHeader(HEADER_SIGNATURE);

        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(signature);
        jws.setKey(remoteRsaJsonWebKey.getRsaPublicKey());

        return jws.verifySignature();
    }
}
