package io.split.engine.common;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public class FetcherOptionsTest {

    @Test
    public void optionsPropagatedOk() {
        final boolean[] called = {false};
        Function<Map<String, String>, Void> func = new Function<Map<String, String>, Void>() {
            @Override
            public  Void apply(Map<String, String> unused) {
                called[0] = true;
                return null;
            }
        };

        FetchOptions options = new FetchOptions.Builder()
                .cacheControlHeaders(true)
                .fastlyDebugHeader(true)
                .responseHeadersCallback(func)
                .build();

        assertEquals(options.cacheControlHeadersEnabled(), true);
        assertEquals(options.fastlyDebugHeaderEnabled(), true);
        options.handleResponseHeaders(new HashMap<>());
        assertEquals(called[0], true);
    }

    @Test
    public void nullHandlerDoesNotExplode() {

        FetchOptions options = new FetchOptions.Builder()
                .cacheControlHeaders(true)
                .fastlyDebugHeader(true)
                .responseHeadersCallback(null)
                .build();

        options.handleResponseHeaders(new HashMap<>());
    }
}
