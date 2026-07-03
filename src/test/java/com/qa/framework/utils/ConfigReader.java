package com.qa.framework.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ConfigReader {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new IllegalStateException("config.properties not found on classpath");
            }
            PROPERTIES.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config.properties", e);
        }
    }

    private ConfigReader() {
    }

    /**
     * Env var takes precedence (e.g. GRID_HUB_URL), falls back to config.properties (grid.hub.url).
     */
    public static String get(String key) {
        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return PROPERTIES.getProperty(key);
    }

    public static String getRequired(String key, String helpMessage) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required config '" + key + "'. " + helpMessage);
        }
        return value;
    }

    public static String getReqresApiKey() {
        return getRequired("reqres.api.key",
                "Set env var REQRES_API_KEY. Get a free key at https://app.reqres.in/api-keys");
    }
}
