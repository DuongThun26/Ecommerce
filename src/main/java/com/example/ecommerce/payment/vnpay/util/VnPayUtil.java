package com.example.ecommerce.payment.vnpay.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public class VnPayUtil {

    private VnPayUtil() {
    }

    /**
     * Tạo chữ ký số HMAC SHA-512 từ chuỗi dữ liệu với secret key được cấp từ VNPay.
     */
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

    /**
     * Encode tham số sang chuẩn URL theo bảng mã US_ASCII theo chuẩn VNPay yêu cầu.
     */
    public static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    /**
     * Gom và sắp xếp các tham số theo thứ tự alphabet (a-z) để tạo chuỗi dữ liệu băm (hash data).
     * Chỉ lấy các trường có giá trị khác null và không rỗng.
     */
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

    /**
     * Sắp xếp và mã hóa cả key lẫn value để tạo query string đầy đủ cho URL chuyển hướng.
     */
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

    /**
     * Xác thực chữ ký số trả về từ VNPay (Return URL hoặc IPN Webhook).
     * Loại bỏ trường hash có sẵn trong params, tạo lại hash và so sánh an toàn bằng MessageDigest.isEqual (chống timing attack).
     */
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