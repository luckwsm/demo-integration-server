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

        return new JsonObject()
                .put("firstName", String.format("Test%d", random))
                .put("lastName", String.format("User%d", random))
                .put("address1", "327 S 87 St")
                .put("email", String.format("%s@email.com", vendorClientAccountId))
                .put("city", "Omaha")
                .put("state", "NE")
                .put("zipCode", "68123")
                .put("vendorClientAccountId", vendorClientAccountId)
                .put("taxEinTinSsn", String.format("333-44-%d", random))
                .put("tinType", "ssn")
                .put("phone", String.format("207555%d", random))
                .put("mobilePhone", String.format("207556%d", random))
                .put("bankName", "Bank of America")
                .put("bankAccountNumber", "12345678")
                .put("bankRoutingNumber", "211274450")
                .put("bankAccountType", "checking")
                .put("dob", "1967-01-01");
    }

    public static byte[] getRoyaltyEarnings(String filename) throws IOException {
        final URL resource = Resources.getResource(filename);
        return Resources.toByteArray(resource);
    }
}
