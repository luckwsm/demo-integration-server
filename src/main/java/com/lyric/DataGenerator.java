package com.lyric;

import com.lyric.models.FileOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.jfairy.Fairy;
import org.jfairy.producer.BaseProducer;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.util.Random;

/**
 * Created by amymadden on 2/10/17.
 */
public class DataGenerator {

    private static Fairy fairy = Fairy.create();
    private static BaseProducer faker = fairy.baseProducer();

    //***********************************SONG SUMMARY (Tunecore) ***********************************

    private static String getTunecoreDistributionSampleHeader(){
        return "sales_period,posted_date,store,country,artists,label,album_type,album_id,album_name,upc,release_date,song_id,song_name,track_num,tunecore_isrc,optional_isrc,distribution_type,units_sold,rev_per_unit,amount_earned\n";
    }

    public static String buildTunecoreDistributionSampleFile(FileOptions options){
        String fileData = getTunecoreDistributionSampleHeader();

        final JsonArray songSummaryJson = buildTunecoreDistributionSampleJson(options);

        for(int i=0;i<songSummaryJson.size();i++){
            JsonObject d = songSummaryJson.getJsonObject(i);

            fileData += String.format("%s,%s,%s,%s,\"%s\",%s,%s,%s,\"%s\",%s,%s,%s,\"%s\",%d,%s,%s,%s,%d,%s,%s\n",
                    d.getString("salesPeriod"), d.getString("postedDate"), d.getString("store"), d.getString("country"), d.getString("artists"),
                    d.getString("label"), d.getString("albumType"), d.getString("albumId"), d.getString("albumName"),
                    d.getString("upc"), d.getString("releaseDate"), d.getString("songId"), d.getString("songName"),
                    d.getInteger("trackNum"), d.getString("tunecoreIsrc"), d.getString("optionalIsrc"), d.getString("distributionType"),
                    d.getInteger("unitsSold"), d.getDouble("revPerUnit"), d.getDouble("amountEarned"));
        }

        return fileData;
    }

    public static JsonArray buildTunecoreDistributionSampleJson(FileOptions options){

        Random rand = new Random();

        int frequencyInDays = options.getFrequencyInDays() != 0 ? options.getFrequencyInDays() : 30;
        int numberOfPeriods = options.getNumberOfPeriods() != 0 ? options.getNumberOfPeriods() : 12;
        int numberOfRecordsPerPeriod = options.getNumberOfRecordsPerPeriod() != 0 ? options.getNumberOfRecordsPerPeriod() : 6;

        int minUnitsSoldPerPeriod = 100;
        int maxUnitsSoldPerPeriod = 200;


        int daysBuffer = 90;
        JsonArray rows = new JsonArray();

        for(int i=0;i<numberOfPeriods;i++){
            final DateTime periodDate = new DateTime().minusDays(daysBuffer + (i*frequencyInDays));
            final DateTime postedDate = periodDate.minusMonths(1);

            for(int j=0;j<numberOfRecordsPerPeriod;j++){
                int unitsSold = rand.nextInt(maxUnitsSoldPerPeriod - minUnitsSoldPerPeriod) + minUnitsSoldPerPeriod;
                rows.add(buildTunecoreDistributionSampleRow(unitsSold, periodDate, postedDate));
            }
        }

        return rows;
    }

    private static JsonObject buildTunecoreDistributionSampleRow(int unitsSold, DateTime salesPeriod, DateTime postedDate){
        DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd");
        double revPerUnit = 0.9688692368900000000000;
        double amountEarned = unitsSold * revPerUnit;

        return createTunecoreDistributionSampleRecord(salesPeriod.toString(dateFormat), postedDate.toString(dateFormat), "iTunes", "AT", "Made in Heights", "HEIGHTS",
                "Album", "1571122", "MADE IN HEIGHTS", "831711486111", "2013-10-31", "1211228", "Skylark Interabang?!", 1, "TCABR5619412", "", "Download", unitsSold,
                revPerUnit, amountEarned);
    }

    public static JsonObject createTunecoreDistributionSampleRecord(String salesPeriod, String postedState, String store, String country, String artists,
                                                                    String label, String albumType, String albumId, String albumName, String upc, String releaseDate,
                                                                    String songId, String songName, int trackNum, String tunecoreIsrc, String optionalIsrc,
                                                                    String distributionType, int unitsSold, double revPerUnit, double amountEarned){
        return new JsonObject()
                .put("salesPeriod", salesPeriod)
                .put("postedDate", postedState)
                .put("store", store)
                .put("country", country)
                .put("artists", artists)
                .put("label", label)
                .put("albumType", albumType)
                .put("albumId", albumId)
                .put("albumName", albumName)
                .put("upc", upc)
                .put("releaseDate", releaseDate)
                .put("songId", songId)
                .put("songName", songName)
                .put("trackNum", trackNum)
                .put("tunecoreIsrc", tunecoreIsrc)
                .put("optionalIsrc", optionalIsrc)
                .put("distributionType", distributionType)
                .put("unitsSold", unitsSold)
                .put("revPerUnit", revPerUnit)
                .put("amountEarned", amountEarned)
                ;
    }

    //***********************************STATEMENT SUMMARY (Sony)***********************************

    public static String buildSonyatvStatementSummaryFile(FileOptions options, JsonObject client){
        String fileData = "";

        final JsonArray statementSummaryJson = buildSonyatvStatementSummaryJson(options, client);

        for(int i=0;i<statementSummaryJson.size();i++){
            JsonObject s = statementSummaryJson.getJsonObject(i);

            String row = String.format("%s,\"%s\",%s,\"%s\",%s,\"%s\",100,100,N,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    s.getString("vendorClientAccountId"), s.getString("name"), s.getString("masterClientId"), s.getString("masterName"),
                    s.getString("apVendorNo"), s.getString("statementDate"), s.getDouble("openingBalanceAmount"), s.getDouble("paymentAmount"),
                    s.getDouble("advanceAmount"), s.getDouble("otherChargesAmount"), s.getDouble("adjustmentsAmount"), s.getDouble("domesticEarningsAmount"),
                    s.getDouble("foreignEarningsAmount"), s.getDouble("closingBalanceAmount"));

            fileData = row + fileData;
        }

        return fileData;
    }

    public static JsonArray buildSonyatvStatementSummaryJson(FileOptions options, JsonObject client){

        final int defaultFequencyInDays = 182;
        final int defaultNumberOfPeriods = 3;
        int frequencyInDays = options.getFrequencyInDays() != 0 ? options.getFrequencyInDays() : defaultFequencyInDays;
        int numberOfPeriods = options.getNumberOfPeriods() != 0 ? options.getNumberOfPeriods() : defaultNumberOfPeriods;

        String masterClientId = client.getJsonObject("userProfile").getJsonObject("vendorAccount").getString("masterClientId");
        String vendorClientAccountId = client.getJsonObject("userProfile").getJsonObject("vendorAccount").getString("vendorClientAccountId");

        int daysBuffer = 90;

        JsonArray rows = new JsonArray();

        BigDecimal openingBalanceAmount = BigDecimal.valueOf(-10000);

        for(int i=numberOfPeriods;i>0;i--){
            final DateTime statementDate = new DateTime().minusDays(daysBuffer + (i*frequencyInDays));
            DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd 00:00:00");

            BigDecimal advanceAmount = BigDecimal.valueOf(0);
            BigDecimal otherChargesAmount = BigDecimal.valueOf(-3955.61);
            BigDecimal adjustmentsAmount = BigDecimal.valueOf(130546.00);
            BigDecimal domesticEarningsAmount = BigDecimal.valueOf(24669.98);
            BigDecimal foreignEarningsAmount = BigDecimal.valueOf(5731.23);
            BigDecimal closingBalanceAmount = openingBalanceAmount.add(advanceAmount).add(otherChargesAmount).add(adjustmentsAmount).add(domesticEarningsAmount).add(foreignEarningsAmount);

            final JsonObject user = client.getJsonObject("userProfile").getJsonObject("user");

            final String name = user.getString("firstName") + user.getString("lastName");
            JsonObject row = createSonyatvStatementSummaryRecord(vendorClientAccountId, name, masterClientId, "", "", statementDate.toString(dateFormat), null, null, null, openingBalanceAmount, null, null, otherChargesAmount, adjustmentsAmount, domesticEarningsAmount, foreignEarningsAmount, closingBalanceAmount);

            rows.add(row);

            openingBalanceAmount = closingBalanceAmount;
        }

        return rows;
    }

    public static JsonObject createSonyatvStatementSummaryRecord(String clientId, String clientName, String payeeId, String payeeName, String apVendorNo,
                                                          String statementDate,  String domesticSharePercent, String foreignSharePercent, String preliminaryBalanceIndicator,
                                                          BigDecimal openingBalanceAmount, BigDecimal paymentAmount, BigDecimal advanceAmount, BigDecimal otherChargesAmount,
                                                          BigDecimal adjustmentsAmount, BigDecimal domesticEarningsAmount, BigDecimal foreignEarningsAmount,
                                                          BigDecimal closingBalanceAmount) {


        return new JsonObject()
                        .put("vendorClientAccountId", clientId)
                        .put("name", clientName)
                        .put("masterClientId", payeeId)
                        .put("masterName", payeeName)
                        .put("apVendorNo", "")
                        .put("statementDate", statementDate)
                        .put("openingBalanceAmount", openingBalanceAmount.doubleValue())
                        .put("paymentAmount", 0)
                        .put("advanceAmount", 0)
                        .put("otherChargesAmount", otherChargesAmount.doubleValue())
                        .put("adjustmentsAmount", adjustmentsAmount.doubleValue())
                        .put("domesticEarningsAmount", domesticEarningsAmount.doubleValue())
                        .put("foreignEarningsAmount", foreignEarningsAmount.doubleValue())
                        .put("closingBalanceAmount", closingBalanceAmount.doubleValue());
    }


}
