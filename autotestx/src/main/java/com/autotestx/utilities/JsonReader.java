package com.autotestx.utilities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.InputStream;
import java.util.List;

/**
 * JSON test data reader and serializer.
 *
 * Reads JSON files from src/test/resources/testdata/json/
 *
 * Usage:
 *   JsonReader.read("books_test_data.json", "validBook")  → Map
 *   JsonReader.readList("users.json", List.class)
 */
public final class JsonReader {

    private static final Logger log = LogManager.getLogger(JsonReader.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private JsonReader() {}

    /**
     * Read a root-level key from a JSON file and return as the specified type.
     */
    public static <T> T readKey(String fileName, String key, Class<T> type) {
        JsonNode root = loadFile(fileName);
        JsonNode node = root.get(key);
        if (node == null) {
            throw new RuntimeException("Key '" + key + "' not found in " + fileName);
        }
        try {
            return MAPPER.treeToValue(node, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize key: " + key, e);
        }
    }

    /**
     * Read entire JSON file and return as the specified type.
     */
    public static <T> T read(String fileName, Class<T> type) {
        try {
            return MAPPER.treeToValue(loadFile(fileName), type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read JSON: " + fileName, e);
        }
    }

    /**
     * Read JSON array as List.
     */
    public static <T> List<T> readList(String fileName, TypeReference<List<T>> typeRef) {
        try (InputStream is = getStream(fileName)) {
            return MAPPER.readValue(is, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read JSON list: " + fileName, e);
        }
    }

    /**
     * Serialize object to JSON string (for request bodies).
     */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    private static JsonNode loadFile(String fileName) {
        try (InputStream is = getStream(fileName)) {
            return MAPPER.readTree(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON file: " + fileName, e);
        }
    }

    private static InputStream getStream(String fileName) {
        String path = "testdata/json/" + fileName;
        InputStream is = JsonReader.class.getClassLoader().getResourceAsStream(path);
        if (is == null) {
            throw new RuntimeException("JSON file not found: " + path);
        }
        return is;
    }
}
