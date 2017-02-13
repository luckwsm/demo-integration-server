package com.lyric;

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

    private static String getSongSummaryHeader(){
        return "sales_period,posted_date,store,country,artists,label,album_type,album_id,album_name,upc,release_date,song_id,song_name,track_num,tunecore_isrc,optional_isrc,distribution_type,units_sold,rev_per_unit,amount_earned\n";
    }

    public static String buildSongSummaryFile(JsonObject options){
        String fileData = getSongSummaryHeader();

        final JsonArray songSummaryJson = buildSongSummaryJson(options);

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

    public static JsonArray buildSongSummaryJson(JsonObject options){

        Random rand = new Random();

        int frequencyInDays = options.getInteger("frequencyInDays", 30);
        int numberOfPeriods = options.getInteger("numberOfPeriods", 12);
        int numberOfRecordsPerPeriod = options.getInteger("numberOfRecordsPerPeriod", 6);
        int minUnitsSoldPerPeriod = options.getInteger("minUnitsSoldPerPeriod", 100);
        int maxUnitsSoldPerPeriod = options.getInteger("maxUnitsSoldPerPeriod", 200);


        int daysBuffer = 90;
        JsonArray rows = new JsonArray();

        for(int i=0;i<numberOfPeriods;i++){
            final DateTime periodDate = new DateTime().minusDays(daysBuffer + (i*frequencyInDays));
            final DateTime postedDate = periodDate.minusMonths(1);

            for(int j=0;j<numberOfRecordsPerPeriod;j++){
                int unitsSold = rand.nextInt(maxUnitsSoldPerPeriod - minUnitsSoldPerPeriod) + minUnitsSoldPerPeriod;
                rows.add(buildSongSummaryRow(unitsSold, periodDate, postedDate));
            }
        }

        return rows;
    }

    private static JsonObject buildSongSummaryRow(int unitsSold, DateTime salesPeriod, DateTime postedDate){
        DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd");
        double revPerUnit = 0.9688692368900000000000;
        double amountEarned = unitsSold * revPerUnit;

        JsonObject row = new JsonObject()
                .put("salesPeriod", salesPeriod.toString(dateFormat))
                .put("postedDate", postedDate.toString(dateFormat))
                .put("store", "iTunes")
                .put("country", "AT")
                .put("artists", "Made in Heights")
                .put("label", "HEIGHTS")
                .put("albumType", "Album")
                .put("albumId", "1071131")
                .put("albumName", "MADE IN HEIGHTS")
                .put("upc", "859711410123")
                .put("releaseDate", "2013-10-31")
                .put("songId", "4219427")
                .put("songName", "Skylark Interabang?!")
                .put("trackNum", 1)
                .put("tunecoreIsrc", "TCABR1319427")
                .put("optionalIsrc", "")
                .put("distributionType", "Download")
                .put("unitsSold", unitsSold)
                .put("revPerUnit", revPerUnit)
                .put("amountEarned", amountEarned)
                ;

        return row;
    }

    //***********************************STATEMENT SUMMARY (Sony)***********************************

    public static String buildStatementSummaryFile(JsonObject options, JsonObject client){
        String fileData = "";

        final JsonArray statementSummaryJson = buildStatementSummaryJson(options, client);

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

    public static JsonArray buildStatementSummaryJson(JsonObject options, JsonObject client){

        int frequencyInDays = options.getInteger("frequencyInDays", 182);
        int numberOfPeriods = options.getInteger("numberOfPeriods", 3);

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

            JsonObject row = new JsonObject()
                    .put("vendorClientAccountId", vendorClientAccountId)
                    .put("name", user.getString("firstName") + user.getString("lastName"))
                    .put("masterClientId", masterClientId)
                    .put("masterName", "")
                    .put("apVendorNo", "")
                    .put("statementDate", statementDate.toString(dateFormat))
                    .put("openingBalanceAmount", openingBalanceAmount.doubleValue())
                    .put("paymentAmount", 0)
                    .put("advanceAmount", 0)
                    .put("otherChargesAmount", otherChargesAmount.doubleValue())
                    .put("adjustmentsAmount", adjustmentsAmount.doubleValue())
                    .put("domesticEarningsAmount", domesticEarningsAmount.doubleValue())
                    .put("foreignEarningsAmount", foreignEarningsAmount.doubleValue())
                    .put("closingBalanceAmount", closingBalanceAmount.doubleValue())
                    ;

            rows.add(row);

            openingBalanceAmount = closingBalanceAmount;
        }

        return rows;
    }

}
