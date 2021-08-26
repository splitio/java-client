package io.split.storages.pluggable.utils;

import io.split.client.utils.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Helper {

    private static final Logger _log = LoggerFactory.getLogger(Helper.class);

    public static long responseToLong(String wrapperResponse, long defaultValue) {
        long response = defaultValue;
        if(wrapperResponse==null) {
            return response;
        }
        try{
            response = Json.fromJson(wrapperResponse, Long.class);
        }
        catch(Exception e) {
            _log.info("Error getting long value from String.");
        }
        return response;
    }
}
