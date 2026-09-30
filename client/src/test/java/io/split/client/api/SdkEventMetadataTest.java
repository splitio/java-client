package io.split.client.api;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SdkEventMetadataTest {

    @Test
    public void updateRetainsNames() {
        SdkEventMetadata m = SdkEventMetadata.update(SdkEventType.FLAGS_UPDATE, Arrays.asList("a", "b"));
        assertEquals(SdkEventType.FLAGS_UPDATE, m.getType());
        assertEquals(2, m.getNames().size());
        assertTrue(m.getNames().contains("a"));
        assertTrue(m.getNames().contains("b"));
    }

    @Test
    public void updateDropsNullAndBlankNames() {
        SdkEventMetadata m = SdkEventMetadata.update(SdkEventType.FLAGS_UPDATE, Arrays.asList("a", null, "", "  "));
        assertEquals(Collections.singleton("a"), m.getNames());
    }

    @Test
    public void updateWithNullCollectionYieldsEmptyNames() {
        assertTrue(SdkEventMetadata.update(SdkEventType.SEGMENTS_UPDATE, null).getNames().isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void namesAreUnmodifiable() {
        SdkEventMetadata.update(SdkEventType.FLAGS_UPDATE, Arrays.asList("a")).getNames().add("b");
    }

    @Test
    public void namesAreDefensivelyCopied() {
        List<String> source = new ArrayList<>(Arrays.asList("a"));
        SdkEventMetadata m = SdkEventMetadata.update(SdkEventType.FLAGS_UPDATE, source);
        source.add("b");
        assertEquals(Collections.singleton("a"), m.getNames());
    }

    @Test
    public void updateHasNoReadyFields() {
        SdkEventMetadata m = SdkEventMetadata.update(SdkEventType.FLAGS_UPDATE, Arrays.asList("a"));
        assertFalse(m.isInitialCacheLoad());
        assertNull(m.getLastUpdateTimestamp());
    }

    @Test
    public void readyStandaloneShape() {
        SdkEventMetadata m = SdkEventMetadata.ready(true, null);
        assertTrue(m.isInitialCacheLoad());
        assertNull(m.getLastUpdateTimestamp());
        assertNull(m.getType());
        assertTrue(m.getNames().isEmpty());
    }

    @Test
    public void readyConsumerShape() {
        SdkEventMetadata m = SdkEventMetadata.ready(false, null);
        assertFalse(m.isInitialCacheLoad());
        assertNull(m.getLastUpdateTimestamp());
    }

    @Test
    public void equalsAndHashCode() {
        SdkEventMetadata a = SdkEventMetadata.update(SdkEventType.FLAGS_UPDATE, Arrays.asList("x"));
        SdkEventMetadata b = SdkEventMetadata.update(SdkEventType.FLAGS_UPDATE, Arrays.asList("x"));
        SdkEventMetadata c = SdkEventMetadata.update(SdkEventType.SEGMENTS_UPDATE, Arrays.asList("x"));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(SdkEventMetadata.ready(true, 1L), SdkEventMetadata.ready(true, 2L));
    }
}
