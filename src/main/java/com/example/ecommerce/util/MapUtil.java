package com.example.ecommerce.util;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


@SuppressWarnings("unchecked")
public class MapUtil {

    private static final Map<Class<?>, Function<String, ?>> TYPE_CONVERTERS = new HashMap<>();

    static {
        TYPE_CONVERTERS.put(String.class, s -> s);
        TYPE_CONVERTERS.put(Integer.class, Integer::parseInt);
        TYPE_CONVERTERS.put(int.class, Integer::parseInt);
        TYPE_CONVERTERS.put(Long.class, Long::parseLong);
        TYPE_CONVERTERS.put(long.class, Long::parseLong);
        TYPE_CONVERTERS.put(BigDecimal.class, BigDecimal::new);
        TYPE_CONVERTERS.put(Double.class, Double::parseDouble);
        TYPE_CONVERTERS.put(double.class, Double::parseDouble);
        TYPE_CONVERTERS.put(Float.class, Float::parseFloat);
        TYPE_CONVERTERS.put(float.class, Float::parseFloat);
        TYPE_CONVERTERS.put(Boolean.class, s -> "true".equalsIgnoreCase(s) || "1".equals(s));
        TYPE_CONVERTERS.put(boolean.class, s -> "true".equalsIgnoreCase(s) || "1".equals(s));
    }

    private MapUtil() {
    }

    public static <T> T get(Map<String, Object> map, String key, Class<T> tClass) {
        return get(map, key, tClass, null);
    }

    public static <T> T get(Map<String, Object> map, String key, Class<T> tClass, T defaultValue) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) {
            return defaultValue;
        }

        Object value = map.get(key);

        // Nếu giá trị đã đúng type, return luôn
        if (tClass.isInstance(value)) {
            return tClass.cast(value);
        }

        try {
            // Nếu là Enum
            if (tClass.isEnum()) {
                return tClass.cast(
                        Enum.valueOf((Class<? extends Enum>) tClass, value.toString().toUpperCase())
                );
            }

            // Nếu là converter đã có
            if (TYPE_CONVERTERS.containsKey(tClass)) {
                String strValue = value.toString().trim();
                if (strValue.isEmpty()) {
                    return defaultValue;
                }
                return tClass.cast(TYPE_CONVERTERS.get(tClass).apply(strValue));
            }

            // Fallback: thử cast trực tiếp
            return tClass.cast(value);

        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static <T> void registerConverter(Class<T> tClass, Function<String, T> converter) {
        TYPE_CONVERTERS.put(tClass, converter);
    }
}
