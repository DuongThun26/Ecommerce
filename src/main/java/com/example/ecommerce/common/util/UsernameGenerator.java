package com.example.ecommerce.common.util;

import java.text.Normalizer;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class UsernameGenerator {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final int MAX_RETRIES = 10;

    private UsernameGenerator() {
    }

    /**
     * Chuẩn hóa họ tên thành chuỗi không dấu, không ký tự đặc biệt, viết thường.
     * Ví dụ: "Nguyễn Văn Anh" -> "nguyenvananh"
     */
    public static String normalizeBaseName(String name) {
        if (name == null || name.isBlank()) {
            return "customer";
        }
        String normalized = Normalizer.normalize(name.trim(), Normalizer.Form.NFD);
        normalized = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        normalized = normalized.replace("đ", "d").replace("Đ", "d");
        normalized = normalized.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        if (normalized.isBlank()) {
            return "customer";
        }
        if (normalized.length() > 20) {
            normalized = normalized.substring(0, 20);
        }
        return normalized;
    }

    /**
     * Tạo username duy nhất bằng cách ghép base name với 4 chữ số ngẫu nhiên.
     * Kiểm tra trùng lặp qua callback existsChecker và thử lại tối đa 10 lần.
     */
    public static String generateUniqueUsername(String name, Function<String, Boolean> existsChecker) {
        String base = normalizeBaseName(name);

        for (int i = 0; i < MAX_RETRIES; i++) {
            int randomSuffix = ThreadLocalRandom.current().nextInt(1000, 10000);
            String candidate = base + randomSuffix;
            if (!Boolean.TRUE.equals(existsChecker.apply(candidate))) {
                return candidate;
            }
        }

        // Fallback: nếu vẫn trùng sau 10 lần, gắn timestamp millisecond (4 chữ số cuối) + 2 số ngẫu nhiên
        long millis = System.currentTimeMillis() % 10000;
        int extra = ThreadLocalRandom.current().nextInt(10, 100);
        return base + millis + extra;
    }
}
