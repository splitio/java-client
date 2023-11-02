package io.split.engine.common;

import org.apache.hc.core5.http.Header;
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
                .targetChangeNumber(123)
                .flagSetsFilter("set1,set2")
                .build();

        assertEquals(options.cacheControlHeadersEnabled(), true);
        assertEquals(options.fastlyDebugHeaderEnabled(), true);
        assertEquals(options.targetCN(), 123);
        assertEquals(called[0], true);
        assertEquals("set1,set2", options.flagSetsFilter());
    }
}
