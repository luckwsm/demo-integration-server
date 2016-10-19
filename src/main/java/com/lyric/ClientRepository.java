package com.lyric;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import com.google.common.io.Resources;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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

        int START = 1000;
        int END = 9999;
        Random r = new Random();
        int random = r.nextInt((END - START) + 1) + START;

        JsonObject user = new JsonObject()
                .put("firstName", String.format("Test%d", random))
                .put("lastName", String.format("User%d", random))
                //.put("address1", "327 S 87 St")
                .put("email", String.format("%s@email.com", random))
                //.put("city", "Omaha")
                //.put("state", "NE")
                //.put("zipCode", "68123")
                //.put("phone", String.format("207-555-%d", random))
                //.put("mobilePhone", String.format("207-556-%d", random))
                //.put("dob", "1967-01-01")
                //.put("gender", "male")
                //.put("maritalStatus", "single")
                ;


        JsonObject vendorAccount = new JsonObject()
                .put("vendorClientAccountId", vendorClientAccountId)
                .put("vendorId", vendorId)
                .put("memberSince", "2007-01-01")
                ;

        if(memberTokenExists){
            //vendorAccount.put("memberToken", String.format("member%d", random));
            vendorAccount.put("memberToken", "1e4bc0a6-043e-4c3e-b067-d5949f310112");
        }

        JsonObject userProfile = new JsonObject()
                .put("user", user)
                .put("vendorAccount", vendorAccount)
                ;

        return new JsonObject()
                .put("userProfile", userProfile);
    }

    public static JsonObject getRoyaltyEarnings(JsonObject fileData, JsonObject options, JsonObject client) throws IOException {
        final String royaltyEarningsContentType = options.getString("royaltyEarningsContentType");
        String fileName = options.getString("filename");
        if(shouldLoadFromFileSystem(royaltyEarningsContentType, fileName)){
            final URL resource = Resources.getResource(fileName);

            fileData
                    .put("data", Resources.toByteArray(resource))
                    .put("filename", fileName)
                    .put("contentType", royaltyEarningsContentType)
            ;
            return fileData;
        }

        try {

            AmazonS3 s3Client = new AmazonS3Client();
            fileName = client.getJsonObject("userProfile").getJsonObject("vendorAccount").getString("vendorClientAccountId");
            logger.info("FILE NOT ON FILESYSTEM, CHECKING S3 FILE NAME: " + fileName);
            S3Object object = s3Client.getObject(new GetObjectRequest("demo-earnings", fileName));
            InputStream objectData = object.getObjectContent();

            final byte[] bytes = IOUtils.toByteArray(objectData);

            fileData.put("contentType", object.getObjectMetadata().getContentType());
            fileData.put("filename", fileName);
            fileData.put("data", bytes);
            objectData.close();
        }
        catch(AmazonServiceException e){
            logger.error("S3 ERROR: " + e.getErrorMessage(), e);
            return fileData;
        }

        return fileData;
    }

    public static boolean fileExistsOnS3(String fileName){
        AmazonS3 s3Client = new AmazonS3Client();
        try {
            s3Client.getObject(new GetObjectRequest("demo-earnings", fileName));
        }
        catch (AmazonServiceException e){
            return false;
        }
        return true;
    }

    private static boolean shouldLoadFromFileSystem(String royaltyEarningsContentType, String fileName) {
        return royaltyEarningsContentType != null && (royaltyEarningsContentType.equals("text/csv") || royaltyEarningsContentType.equals("application/zip") ) && !fileName.equals("");
    }
}
