package com.lyric;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import com.google.common.io.Resources;
import com.lyric.models.CsvSchemaType;
import com.lyric.models.FileOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by amymadden on 3/9/17.
 */
public class FileDataRepository {
    static Logger logger = LoggerFactory.getLogger(FileDataRepository.class.getName());

    public static JsonArray getFileRecords(FileOptions fileOptions, JsonObject client) {

        JsonArray fileRecords = new JsonArray();

        for (String schema : fileOptions.getSchemas()) {
            final CsvSchemaType csvSchemaType = CsvSchemaType.valueOf(schema);
            fileRecords.add(getFileRecord(csvSchemaType.getFileType(), csvSchemaType.name(), csvSchemaType.getAdditionalJweHeaders(), fileOptions, client));
        }

        return fileRecords;

    }

    public static JsonArray getFileRecordsJson(String csvSchema, FileOptions fileOptions, JsonObject client){
        JsonArray fileRecordsJson = new JsonArray();
        String fileName = getFileName(csvSchema, client);
        //String fileName = "amyLyricTest-SonyatvStatementSummary.csv";
        byte[] fileData = getFileData(fileName, csvSchema, fileOptions, client);

        String fileDataString = new String(fileData);
        String[] fileDataRows = fileDataString.split("\n");



        switch (csvSchema) {
            case "TunecoreDistributionSample":
                //remove header row
                String[] dataRows = Arrays.copyOfRange(fileDataRows, 1, fileDataRows.length);

                HashMap<String, JsonObject> hashMap = aggregateRowsBySalesPeriod(dataRows);
                for (JsonObject entry : hashMap.values()) {
                    fileRecordsJson.add(entry);
                }

                break;
            case "SonyatvStatementSummary":
                for (String fileDataRow : fileDataRows) {
                    String[] fileDataRowParts = fileDataRow.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                    JsonObject fileDataRowJson = DataGenerator.createSonyatvStatementSummaryRecord(cleanString(fileDataRowParts[0]), cleanString(fileDataRowParts[1]), cleanString(fileDataRowParts[2]),
                            cleanString(fileDataRowParts[3]), cleanString(fileDataRowParts[4]), cleanString(fileDataRowParts[5]), cleanString(fileDataRowParts[6]), cleanString(fileDataRowParts[7]),
                            cleanString(fileDataRowParts[8]), bigDecimalOf(fileDataRowParts[9]), bigDecimalOf(fileDataRowParts[10]), bigDecimalOf(fileDataRowParts[11]), bigDecimalOf(fileDataRowParts[12]),
                            bigDecimalOf(fileDataRowParts[13]), bigDecimalOf(fileDataRowParts[14]), bigDecimalOf(fileDataRowParts[15]), bigDecimalOf(fileDataRowParts[16]));

                    fileRecordsJson.add(fileDataRowJson);
                }
                break;
        }

        return fileRecordsJson;
    }

    private static HashMap<String, JsonObject> aggregateRowsBySalesPeriod(String[] dataRows) {
        HashMap<String, JsonObject> hashMap = new HashMap<String, JsonObject>();

        for (String fileDataRow : dataRows) {
            String[] fileDataRowParts = fileDataRow.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

            final String salesPeriod = fileDataRowParts[0];
            final double amountEarned = Double.parseDouble(fileDataRowParts[19]);
            final int unitsSold = Integer.parseInt(fileDataRowParts[17]);

            if (!hashMap.containsKey(salesPeriod)) {
                JsonObject fileDataRowJson = DataGenerator.createTunecoreDistributionSampleRecord(salesPeriod, unitsSold, amountEarned);

                hashMap.put(salesPeriod, fileDataRowJson);
            } else {
                final JsonObject fileDataRowJson = hashMap.get(salesPeriod);
                int cumulativeUnitsSold = fileDataRowJson.getInteger("unitsSold") + unitsSold;
                fileDataRowJson.put("unitsSold", cumulativeUnitsSold);

                double cumulativeAmountEarned = fileDataRowJson.getInteger("amountEarned") + amountEarned;
                fileDataRowJson.put("amountEarned", cumulativeAmountEarned);
            }

        }
        return hashMap;
    }

    private static String getFileName(String csvSchema, JsonObject client) {
        String fileName = client.getJsonObject("userProfile").getJsonObject("vendorAccount").getString("vendorClientAccountId");

        final String masterClientId = client.getJsonObject("userProfile").getJsonObject("vendorAccount").getString("masterClientId", null);

        if(masterClientId != null){
            fileName += "-" + masterClientId;
        }
        fileName += "-" + csvSchema + ".csv";
        return fileName;
    }

    private static BigDecimal bigDecimalOf(String value){
        return BigDecimal.valueOf(Double.parseDouble(value));
    }

    private static String cleanString(String value){
        if(value.length() == 0 || !value.substring(0, 1).equals("\"")){
            return value;
        }

        return value.substring(1, value.length()-1);
    }

    private static JsonObject getFileRecord(String fileType, String csvSchema, JsonArray additionalJweHeaders, FileOptions fileOptions, JsonObject client){
        String contentType = "text/csv";

        String fileName = getFileName(csvSchema, client);

        if(fileOptions.getSpecifiedFileName() != null){
            fileName = fileOptions.getSpecifiedFileName();
            contentType = fileOptions.getSpecifiedFileContentType();
        }

        byte[] fileData = getFileData(fileName, csvSchema, fileOptions, client);

        return new JsonObject()
                .put("filename", fileName)
                .put("contentType", contentType)
                .put("data", fileData)
                .put("filesetFileType", fileType)
                .put("csvSchema", csvSchema)
                .put("additionalJweHeaders", additionalJweHeaders);
    }

    private static byte[] getFileData(String fileName, String csvSchema, FileOptions fileOptions, JsonObject client){
        byte[] fileData = loadFromFileSystem(fileName);

        if(fileData != null){
            return fileData;
        }

        fileData = loadFromS3(fileName);

        if(fileData != null){
            return fileData;
        }

        return generateFileData(csvSchema, fileOptions, client);
    }

    private static byte[] loadFromFileSystem(String fileName){
        byte[] fileData = null;
        try {
            URL resource = Resources.getResource(fileName);
            fileData = Resources.toByteArray(resource);
        }
        catch(IllegalArgumentException e){
            logger.info("file " + fileName + " doesn't exist on file system");
        } catch (IOException e) {
            logger.info("error loading file " + fileName + " from file system");
        }
        return fileData;
    }

    private static byte[] loadFromS3(String fileName){
        byte[] fileData = null;
        try {

            AmazonS3 s3Client = new AmazonS3Client();
            S3Object object = s3Client.getObject(new GetObjectRequest("demo-earnings", fileName));
            InputStream objectData = object.getObjectContent();

            fileData = IOUtils.toByteArray(objectData);

            objectData.close();
        }
        catch(AmazonServiceException e){
            logger.error("error loading file " + fileName + "from s3 : " + e.getErrorMessage(), e);
        } catch (IOException e) {
            logger.info("error loading file " + fileName + " from s3");
        }

        return fileData;
    }

    private static byte[] generateFileData(String csvSchema, FileOptions fileOptions, JsonObject client){
        byte[] fileData = null;
        switch (csvSchema){
            case "TunecoreDistributionSample":
                fileData = DataGenerator.buildTunecoreDistributionSampleFile(fileOptions).getBytes();
                break;
            case "SonyatvStatementSummary":
                fileData = DataGenerator.buildSonyatvStatementSummaryFile(fileOptions, client).getBytes();
                break;
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
