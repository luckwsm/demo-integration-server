package com.lyric;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.net.URL;
import java.util.Random;

/**
 * Created by amadden on 1/20/16.
 */
public class ClientRepository {

    /* This gets a unique user every time for testing purposes.  This would really be a lookup from
    your database.
     */
    public static JsonObject findClient(String vendorClientAccountId){

        int START = 1000;
        int END = 9999;
        Random r = new Random();
        int random = r.nextInt((END - START) + 1) + START;

        JsonObject user = new JsonObject()
                .put("firstName", String.format("Test%d", random))
                .put("lastName", String.format("User%d", random))
                .put("address1", "327 S 87 St")
                .put("email", String.format("%s@email.com", random))
                .put("city", "Omaha")
                .put("state", "NE")
                .put("zipCode", "68123")
                .put("phone", String.format("207-555-%d", random))
                .put("mobilePhone", String.format("207-556-%d", random))
                .put("dob", "1967-01-01")
                .put("gender", "male")
                .put("maritalStatus", "single")
                ;

        JsonObject bankInfo = new JsonObject()
                .put("bankName", "Bank of America")
                .put("bankAccountNumber", "12345678")
                .put("bankRoutingNumber", "211274450")
                .put("bankAccountType", "checking")
                ;

        JsonObject taxInfo = new JsonObject()
                .put("taxEinTinSsn", String.format("333-44-%d", random))
                .put("tinType", "ssn")
                .put("memberBusinessType", "individual")
                ;

        JsonObject vendorAccount = new JsonObject()
                .put("vendorClientAccountId", String.format("client%d", random))
                ;

        return new JsonObject()
                .put("user", user)
                .put("vendorAccount", vendorAccount)
                .put("taxInfo", taxInfo)
                .put("bankInfo", bankInfo)
                ;
    }

    public static byte[] getRoyaltyEarnings(String filename) throws IOException {
        final URL resource = Resources.getResource(filename);
        return Resources.toByteArray(resource);
    }
}
