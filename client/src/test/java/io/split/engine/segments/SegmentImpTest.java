package io.split.engine.segments;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SegmentImpTest extends TestCase {
    private static final String SEGMENT_NAME = "TestSegment";
    private static final long CHANGE_NUMBER = 123L;
    private static final long NEW_CHANGE_NUMBER = 321L;
    private static final String KEY = "KEYTEST";
    private static final String FAKE_KEY = "FAKE_KEY";

    @Test
    public void testSegmentName() {
        SegmentImp segmentImp = new SegmentImp(CHANGE_NUMBER, SEGMENT_NAME);
        assertEquals(SEGMENT_NAME, segmentImp.getName());
    }

    @Test
    public void testContainsWithSuccess() {
        SegmentImp segmentImp = new SegmentImp(CHANGE_NUMBER, SEGMENT_NAME, Stream.of(KEY).collect(Collectors.toList()));
        assertTrue(segmentImp.contains(KEY));
    }

    @Test
    public void testContainsWithNoSuccess() {
        SegmentImp segmentImp = new SegmentImp(CHANGE_NUMBER, SEGMENT_NAME, Stream.of(KEY).collect(Collectors.toList()));
        assertFalse(segmentImp.contains(FAKE_KEY));
    }

    @Test
    public void testChangeNumber(){
        SegmentImp segmentImp = new SegmentImp(CHANGE_NUMBER, SEGMENT_NAME);
        assertEquals(CHANGE_NUMBER, segmentImp.getChangeNumber());
    }

    @Test
    public void testSetChangeNumber(){
        SegmentImp segmentImp = new SegmentImp(CHANGE_NUMBER, SEGMENT_NAME);
        segmentImp.setChangeNumber(NEW_CHANGE_NUMBER);
        assertEquals(NEW_CHANGE_NUMBER, segmentImp.getChangeNumber());
    }

    @Test
    public void testUpdateSegment(){
        SegmentImp segmentImp = new SegmentImp(CHANGE_NUMBER, SEGMENT_NAME);
        segmentImp.update(Stream.of(KEY).collect(Collectors.toList()), new ArrayList<>());
        assertTrue(segmentImp.contains(KEY));
    }

}