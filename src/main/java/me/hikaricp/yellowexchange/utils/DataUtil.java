package me.hikaricp.yellowexchange.utils;

import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public class DataUtil {

    public double getDouble(Map<String, Object> body, String key) {
        if (body.containsKey(key)) {
            String value = String.valueOf(body.get(key));
            try {
                double amount = Double.parseDouble(value);
                if (Double.isNaN(amount)) {
                    return 0D;
                } else {
                    return amount;
                }
            } catch (Exception ignored) {}
        }

        return 0D;
    }

    public boolean getBoolean(Map<String, Object> body, String key) {
        if (body.containsKey(key)) {
            String value = String.valueOf(body.get(key));
            try {
                return Boolean.parseBoolean(value);
            } catch (Exception ignored) {}
        }

        return false;
    }

    public boolean getBoolean(Map<String, Object> body, String key, boolean defaultValue) {
        if (body.containsKey(key)) {
            String value = String.valueOf(body.get(key));
            try {
                Boolean.parseBoolean(value);
            } catch (Exception ignored) {}
        }

        return defaultValue;
    }
}
