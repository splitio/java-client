package io.split.engine.common;

import org.junit.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class FastlyHeadersCaptorTest {

    @Test
    public void filterWorks() {
        FastlyHeadersCaptor captor = new FastlyHeadersCaptor();
        captor.handle(Stream.of(new String[][] {
                {"Fastly-Debug-Path", "something"},
                {"Fastly-Debug-TTL", "something"},
                {"Fastly-Debug-Digest", "something"},
                {"X-Served-By", "something"},
                {"X-Cache", "something"},
                {"X-Cache-Hits", "something"},
                {"X-Timer", "something"},
                {"Surrogate-Key", "something"},
                {"ETag", "something"},
                {"Cache-Control", "something"},
                {"X-Request-ID", "something"},
                {"Last-Modified", "something"},
                {"NON_IMPORTANT_1", "something"},
                {"ANOTHER_NON_IMPORTANT", "something"}
        }).collect(Collectors.toMap(d -> d[0], d -> d[1])));

        assertEquals(captor.get().size(), 1);
        assertEquals(captor.get().get(0).size(), 12);
        assertFalse(captor.get().get(0).containsKey("NON_IMPORTANT_1"));
        assertFalse(captor.get().get(0).containsKey("ANOTHER_NON_IMPORTANT"));
    }

    @Test
    public void orderIsPreserved() {
        FastlyHeadersCaptor captor = new FastlyHeadersCaptor();
        captor.handle(Stream.of(new String[][]{
                {"Fastly-Debug-Path", "first"},
        }).collect(Collectors.toMap(d -> d[0], d -> d[1])));

        captor.handle(Stream.of(new String[][]{
                {"Fastly-Debug-Path", "second"},
        }).collect(Collectors.toMap(d -> d[0], d -> d[1])));

        captor.handle(Stream.of(new String[][]{
                {"Fastly-Debug-Path", "third"},
        }).collect(Collectors.toMap(d -> d[0], d -> d[1])));

        assertEquals(captor.get().size(), 3);
        assertEquals(captor.get().get(0).size(), 1);
        assertEquals(captor.get().get(1).size(), 1);
        assertEquals(captor.get().get(2).size(), 1);
        assertEquals(captor.get().get(0).get("Fastly-Debug-Path"), "first");
        assertEquals(captor.get().get(1).get("Fastly-Debug-Path"), "second");
        assertEquals(captor.get().get(2).get("Fastly-Debug-Path"), "third");
    }
}
