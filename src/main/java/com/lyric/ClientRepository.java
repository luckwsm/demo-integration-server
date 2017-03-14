package com.lyric;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import com.google.common.io.Resources;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by amadden on 1/20/16.
 */
public class ClientRepository {
    static Logger logger = LoggerFactory.getLogger(ClientRepository.class.getName());
    /* This gets a unique user every time for testing purposes.  This would really be a lookup from
    your database.
     */
    public static JsonObject findClient(String vendorClientAccountId, Boolean memberTokenExists, String vendorId){
        return findClient(vendorClientAccountId, memberTokenExists, vendorId, null);
    }

    public static JsonObject findClient(String vendorClientAccountId, Boolean memberTokenExists, String vendorId, JsonObject clientData){

        if(vendorClientAccountId.equals("productionSmokeTestUser")){
            return getProductionUser();
        }

        if(clientData != null){
            return findExistingClient(vendorClientAccountId, vendorId, clientData);
        }

        return generateNewRandomClient(vendorClientAccountId, memberTokenExists, vendorId);
    }

    private static JsonObject findExistingClient(String vendorClientAccountId, String vendorId, JsonObject clientData){
        return createClient(vendorClientAccountId, vendorId, clientData.getString("email"), clientData.getString("firstName"),
                clientData.getString("lastName"), clientData.getString("memberSince", "2007-01-01"), clientData.getInteger("paymentTerms", 90),
                clientData.getString("distributionCycle", "semiannual"));
    }

    private static JsonObject createClient(String vendorClientAccountId, String vendorId, String email, String firstName,
                                           String lastName, String memberSince, int paymentTerms, String distributionCycle){
        JsonObject user = new JsonObject()
                .put("firstName", firstName)
                .put("lastName", lastName)
                .put("email", email)
                ;

        JsonObject vendorAccount = new JsonObject()
                .put("vendorClientAccountId", vendorClientAccountId)
                .put("vendorId", vendorId)
                .put("memberSince", memberSince)
                .put("paymentTerms", paymentTerms)
                .put("distributionCycle", distributionCycle)
                ;

        JsonObject userProfile = new JsonObject()
                .put("user", user)
                .put("vendorAccount", vendorAccount)
                ;

        return new JsonObject()
                .put("userProfile", userProfile);
    }

    private static JsonObject generateNewRandomClient(String vendorClientAccountId, Boolean memberTokenExists, String vendorId) {
        int START = 1000;
        int END = 9999;
        Random r = new Random();
        int random = r.nextInt((END - START) + 1) + START;

        JsonObject userProfile = createClient(vendorClientAccountId, vendorId, String.format("%s@email.com", random),
                String.format("Test%d", random), String.format("User%d", random), "2007-01-01", 90, "semiannual");

        if(memberTokenExists){
            userProfile.getJsonObject("userProfile").getJsonObject("vendorAccount").put("memberToken", "1e4bc0a6-043e-4c3e-b067-d5949f310112");
        }

        return userProfile;
    }

    private static JsonObject getProductionUser() {
        JsonObject user = new JsonObject()
                .put("firstName", String.format("Test%s", "Smoke"))
                .put("lastName", String.format("User%s", "Test"))
                .put("email", String.format("%s@email.com", "smokeTest"))
                ;


        JsonObject vendorAccount = new JsonObject()
                .put("vendorClientAccountId", "productionSmokeTestUser")
                .put("vendorId", "demo")
                ;

        JsonObject userProfile = new JsonObject()
                .put("user", user)
                .put("vendorAccount", vendorAccount)
                ;

        return new JsonObject()
                .put("userProfile", userProfile);
    }

}
