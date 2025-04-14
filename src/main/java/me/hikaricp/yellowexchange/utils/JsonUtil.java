package me.hikaricp.yellowexchange.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @SneakyThrows
    public <T> T readJson(String json, Class<?> typeClass) {
        return (T) OBJECT_MAPPER.readValue(json, typeClass);
    }

    @SneakyThrows
    public String writeJson(Object value) {
        return OBJECT_MAPPER.writeValueAsString(value);
    }

    @SneakyThrows
    public String writeJsonPretty(Object value) {
        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    }
}
