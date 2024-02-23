package io.split.engine.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FetcherOptionsTest {

    @Test
    public void optionsPropagatedOk() {
        FetchOptions options = new FetchOptions.Builder()
                .cacheControlHeaders(true)
                .targetChangeNumber(123)
                .flagSetsFilter("set1,set2")
                .build();

        assertEquals(options.cacheControlHeadersEnabled(), true);
        assertEquals(options.targetCN(), 123);
        assertEquals("set1,set2", options.flagSetsFilter());
    }
}
