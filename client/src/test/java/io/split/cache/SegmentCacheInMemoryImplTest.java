package io.split.cache;

import io.split.cache.SegmentCacheInMemoryImpl;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SegmentCacheInMemoryImplTest extends TestCase {
    private static final String SEGMENT_NAME = "TestSegment";
    private static final String FAKE_SEGMENT_NAME = "FakeSegment";
    private static final long CHANGE_NUMBER = 123L;
    private static final String KEY = "KEYTEST";
    private static final long DEFAULT_CHANGE_NUMBER = -1L;

    @Test
    public void testUpdateSegment(){
        SegmentCacheInMemoryImpl segmentCacheInMemory = new SegmentCacheInMemoryImpl();
        segmentCacheInMemory.updateSegment(SEGMENT_NAME,new ArrayList<>(), new ArrayList<>());

        assertEquals(DEFAULT_CHANGE_NUMBER, segmentCacheInMemory.getChangeNumber(SEGMENT_NAME));
    }

    @Test
    public void testIsInSegment() {
        SegmentCacheInMemoryImpl segmentCacheInMemory = new SegmentCacheInMemoryImpl();
        segmentCacheInMemory.updateSegment(SEGMENT_NAME, Stream.of(KEY).collect(Collectors.toList()), new ArrayList<>());
        assertTrue(segmentCacheInMemory.isInSegment(SEGMENT_NAME, KEY));
    }
    @Test
    public void testIsInSegmentWithFakeSegment() {
        SegmentCacheInMemoryImpl segmentCacheInMemory = new SegmentCacheInMemoryImpl();
        segmentCacheInMemory.updateSegment(SEGMENT_NAME, Stream.of(KEY).collect(Collectors.toList()), new ArrayList<>());
        assertFalse(segmentCacheInMemory.isInSegment(FAKE_SEGMENT_NAME, KEY));
    }

    @Test
    public void testSetChangeNumber() {
        SegmentCacheInMemoryImpl segmentCacheInMemory = new SegmentCacheInMemoryImpl();
        segmentCacheInMemory.updateSegment(SEGMENT_NAME,new ArrayList<>(), new ArrayList<>());
        segmentCacheInMemory.setChangeNumber(SEGMENT_NAME, CHANGE_NUMBER);
        assertEquals(CHANGE_NUMBER, segmentCacheInMemory.getChangeNumber(SEGMENT_NAME));
    }

    @Test
    public void testGetChangeNumberWithFakeSegment() {
        SegmentCacheInMemoryImpl segmentCacheInMemory = new SegmentCacheInMemoryImpl();
        segmentCacheInMemory.updateSegment(SEGMENT_NAME,new ArrayList<>(), new ArrayList<>());
        assertEquals(DEFAULT_CHANGE_NUMBER, segmentCacheInMemory.getChangeNumber(FAKE_SEGMENT_NAME));
    }

    @Test
    public void testClear() {
        SegmentCacheInMemoryImpl segmentCacheInMemory = new SegmentCacheInMemoryImpl();
        segmentCacheInMemory.updateSegment(SEGMENT_NAME,new ArrayList<>(), new ArrayList<>());
        segmentCacheInMemory.setChangeNumber(SEGMENT_NAME, CHANGE_NUMBER);
        segmentCacheInMemory.clear();
        assertEquals(DEFAULT_CHANGE_NUMBER, segmentCacheInMemory.getChangeNumber(SEGMENT_NAME));
    }
}