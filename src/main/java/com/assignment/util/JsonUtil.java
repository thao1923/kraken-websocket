package com.assignment.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;


@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonUtil {
    private static Gson gson;
    private static Gson gsonIncludeNulls;

    static {
        gson = objectMapper();
        gsonIncludeNulls = objectMapperIncludeNulls();
    }

    private static Gson objectMapper(){
        return new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }

    private static Gson objectMapperIncludeNulls(){
        return new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .disableHtmlEscaping()
                .create();
    }



    public static String toJson(Object object){
        return toJson(object, false);
    }

    public static String toJson(Object object, boolean includeEmptyAttributes){
        if (includeEmptyAttributes){
            return gsonIncludeNulls.toJson(object);
        }
        return gson.toJson(object);
    }

    public static <T> T parseJson(String jsonString, Class<T> clazz){
        return gson.fromJson(jsonString, clazz);
    }

    public static <T> T parseJson(String jsonString, Class<T> outer, Class<?>... parameterized){
        Type type = TypeToken.getParameterized(outer, parameterized).getType();
        return gson.fromJson(jsonString, type);
    }

}
