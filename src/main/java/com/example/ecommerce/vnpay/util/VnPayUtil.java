package com.example.ecommerce.vnpay.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public class VnPayUtil {

    private VnPayUtil() {
    }

    public static String hmacSHA512(String secretKey, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec =
                    new SecretKeySpec(
                            secretKey.getBytes(StandardCharsets.UTF_8),
                            "HmacSHA512"
                    );

            hmac512.init(secretKeySpec);
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot generate VNPAY HMAC SHA512", e);
        }
    }

    public static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    public static String buildHashData(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (!hashData.isEmpty()) {
                    hashData.append("&");
                }
                hashData.append(fieldName)
                        .append("=")
                        .append(encode(fieldValue));
            }
        }
        return hashData.toString();
    }

    public static String buildQueryString(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (!query.isEmpty()) {
                    query.append("&");
                }
                query.append(encode(fieldName))
                        .append("=")
                        .append(encode(fieldValue));
            }
        }
        return query.toString();
    }

    public static boolean verifySecureHash(Map<String, String> params, String receivedHash, String hashSecret) {
        if (receivedHash == null
                || receivedHash.isBlank()) {
            return false;
        }
        Map<String, String> data = new HashMap<>(params);
        data.remove("vnp_SecureHash");
        data.remove("vnp_SecureHashType");
        String hashData = buildHashData(data);
        String calculatedHash = hmacSHA512(hashSecret, hashData);

        return MessageDigest.isEqual(calculatedHash.getBytes(StandardCharsets.US_ASCII), receivedHash.getBytes(StandardCharsets.US_ASCII));
    }
}