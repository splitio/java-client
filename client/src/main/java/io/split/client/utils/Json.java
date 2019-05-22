package io.split.client.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Created by adilaijaz on 6/10/16.
 */
public class Json {

    private static final Gson _json = new GsonBuilder()
            .serializeNulls()  // Send nulls
            .registerTypeAdapter(Double.class, new JsonSerializer<Double>() {
                @Override

                // Send integers as such
                public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                    if (src == src.longValue())
                        return new JsonPrimitive(src.longValue());
                    return new JsonPrimitive(src);
                }
            })
            .create();

    public static String toJson(Object obj) {
        return _json.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clz) {
        return _json.fromJson(json, clz);
    }

}
