package io.split.engine.common;

import org.junit.Test;

import java.util.Map;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public class FetcherOptionsTest {

    @Test
    public void optionsPropagatedOk() {
        FetchOptions options = new FetchOptions.Builder()
                .cacheControlHeaders(true)
                .fastlyDebugHeader(true)
                .targetChangeNumber(123)
                .flagSetsFilter("set1,set2")
                .build();

        assertEquals(options.cacheControlHeadersEnabled(), true);
        assertEquals(options.fastlyDebugHeaderEnabled(), true);
        assertEquals(options.targetCN(), 123);
        assertEquals("set1,set2", options.flagSetsFilter());
    }
}
