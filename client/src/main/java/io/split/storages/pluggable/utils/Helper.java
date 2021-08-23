package io.split.storages.pluggable.utils;

import io.split.client.utils.Json;

public class Helper {

    public static long responseToLong(String wrapperResponse, long defaultValue) {
        long response = defaultValue;
        if(wrapperResponse==null) {
            return response;
        }
        try{
            response = Json.fromJson(wrapperResponse, Long.class);
        }
        catch(Exception e) {
//            _log.info("Error getting long value from String.");
        }
        return response;
    }
}
