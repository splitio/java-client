package io.split.client.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Created by adilaijaz on 6/10/16.
 */
public class Json {

    private static final Gson _json = new GsonBuilder()
            .serializeNulls()
            .create();

    public static String toJson(Object obj) {
        return _json.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clz) {
        return _json.fromJson(json, clz);
    }

}
