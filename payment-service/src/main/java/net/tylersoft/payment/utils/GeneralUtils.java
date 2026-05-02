package net.tylersoft.payment.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GeneralUtils {

    public static String generateHashedKey(Map<String, Object> payload, String key){
        List<String> keys = new ArrayList<>(payload.keySet());
        keys = keys.stream().sorted(String::compareTo).toList();
        StringBuilder concatenatedString = new StringBuilder(key);

        for (String k : keys) {
            concatenatedString.append(payload.get(k));
        }
        return generateSHA256Hash(concatenatedString.toString());
    }

    public static String  generateSHA256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Compute the hash
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert the byte array to a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0'); // Ensure two-character hex
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException("Could not complete request with error " + e.getMessage());
        }

    }
}
