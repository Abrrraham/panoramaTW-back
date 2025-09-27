package com.panorama.backend.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-24 14:30:37
 * @version: 1.0
 */
public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static JsonNode parseJson(File jsonFile) throws IOException {
        // 将 JSON 字符串解析为 JsonNode
        return objectMapper.readTree(jsonFile);
    }

    public static String serializeObject(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    public static ObjectNode getObjectNode() {
        return objectMapper.createObjectNode();
    }

    public static Map<String, Object> jsonToMap(JsonNode jsonNode){
        // 处理空字符串
        jsonNode.fields().forEachRemaining(entry -> {
            if (entry.getValue().isTextual() && entry.getValue().asText().isEmpty()) {
                ((ObjectNode) jsonNode).put(entry.getKey(), (String) null); // 将空字符串设置为 null
            }
        });
        return objectMapper.convertValue(jsonNode, new TypeReference<Map<String, Object>>() {});
    }

    public static JsonNode mapToJson(Map<?,?> map){
        return objectMapper.valueToTree(map);
    }

    public static String toJsonString(Object json) {
        if (null == json) return "";

        String jsonStr = "";

        // JSONObject,JSONArray 都实现了JSON接口
        Map<String, String> map = new HashMap<>(1);
        map.put("JSON", json.toString());
        try {
            jsonStr = objectMapper.writeValueAsString(map.get("JSON"));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "";
        }

        return jsonStr;
    }

    public static <T> T jsonToObject(String json, Class<T> clazz) throws IOException {
        // 将 JSON 字符串反序列化为指定的 Java 对象
        return objectMapper.readValue(json, clazz);
    }
}
