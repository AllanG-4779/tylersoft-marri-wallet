package net.tylersoft.wallet.utils;

import java.time.LocalDate;

public class CoreWalletUtils {

    public static String generate(int accountId, String accountPrefix, int expLength, boolean julian) {

        // Step 1 - Julian day
        int yearDays = 0;
        if (julian) {
            // Equivalent of TO_DAYS(NOW()) + 1721060
            long epochDays = LocalDate.now().toEpochDay(); // days since 1970-01-01
            yearDays = (int) (epochDays + 2440588);        // shift to Julian Day Number
        }

        // Step 2 - Build partial number
        String partialNo = accountPrefix + (julian ? yearDays : "0");

        // Step 3 - Calculate padding
        String accountIdStr = String.valueOf(accountId);
        int diffLength = expLength - (partialNo.length() + accountIdStr.length());

        // Step 4 - Assemble
        if (diffLength > 0) {
            String padding = "0".repeat(diffLength);
            return partialNo + padding + accountIdStr;
        } else {
            return partialNo + accountIdStr;
        }
    }
}

