package com.autotestx.utilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads environment-specific configuration from:
 *   src/test/resources/config/{env}.properties
 *
 * Env is resolved via system property "-Denv=qa" (default: qa).
 *
 * Usage:
 *   ConfigReader.get("base.url")
 *   ConfigReader.get("db.url")
 */
public final class ConfigReader {

    private static final Logger log = LogManager.getLogger(ConfigReader.class);
    private static final Properties PROPS = new Properties();

    static {
        String env = System.getProperty("env", "qa");
        String configFile = "config/" + env + ".properties";
        log.info("Loading configuration: {}", configFile);

        try (InputStream is = ConfigReader.class.getClassLoader().getResourceAsStream(configFile)) {
            if (is == null) {
                throw new RuntimeException("Config file not found: " + configFile);
            }
            PROPS.load(is);
            log.info("Loaded {} properties for environment: {}", PROPS.size(), env);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config: " + configFile, e);
        }
    }

    private ConfigReader() {}

    public static String get(String key) {
        String value = PROPS.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Missing config property: " + key);
        }
        return value.trim();
    }

    public static String get(String key, String defaultValue) {
        return PROPS.getProperty(key, defaultValue).trim();
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public static long getLong(String key) {
        return Long.parseLong(get(key));
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public static String getEnv() {
        return System.getProperty("env", "qa");
    }
}
