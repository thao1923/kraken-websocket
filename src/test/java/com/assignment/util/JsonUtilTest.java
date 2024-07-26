package com.assignment.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonUtilTest {
    static Map<String, String> map = null;
    static String mapAsJson =null;
    static String mapAsJsonWithEmptyAttributes = null;

    static{
        map =new HashMap<>();
        map.put("name", "xxxx");
        map.put("age", null);

        mapAsJson = "{\"name\":\"xxxx\"}";
        mapAsJsonWithEmptyAttributes = "{\"name\":\"xxxx\", \"age\":null}";
    }

    @Test
    void parseJsonTest(){
        Map<String, String> parsedMap = JsonUtil.parseJson(mapAsJson, Map.class);
        assertEquals(1, parsedMap.size());

        parsedMap = JsonUtil.parseJson(mapAsJson, Map.class, String.class, String.class);
        assertEquals(1, parsedMap.size());
    }

    @Test
    void testToJson(){
        String strMap = JsonUtil.toJson(map);
        assertTrue(strMap.contains("name"));
    }

    @Test
    void testToJson_emptyAttributes(){
        String strMap = JsonUtil.toJson(map, true);
        assertTrue(strMap.contains("age"));
    }
}