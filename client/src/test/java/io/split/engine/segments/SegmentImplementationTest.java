package io.split.engine.segments;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SegmentImplementationTest extends TestCase {
    private static final String SEGMENT_NAME = "TestSegment";
    private static final long CHANGE_NUMBER = 123L;
    private static final long NEW_CHANGE_NUMBER = 321L;
    private static final String KEY = "KEYTEST";
    private static final String FAKE_KEY = "FAKE_KEY";

    @Test
    public void testSegmentName() {
        SegmentImplementation segmentImplementation = new SegmentImplementation(CHANGE_NUMBER, SEGMENT_NAME);
        assertEquals(SEGMENT_NAME, segmentImplementation.segmentName());
    }

    @Test
    public void testContainsWithSuccess() {
        SegmentImplementation segmentImplementation = new SegmentImplementation(CHANGE_NUMBER, SEGMENT_NAME, Stream.of(KEY).collect(Collectors.toList()));
        assertTrue(segmentImplementation.contains(KEY));
    }

    @Test
    public void testContainsWithNoSuccess() {
        SegmentImplementation segmentImplementation = new SegmentImplementation(CHANGE_NUMBER, SEGMENT_NAME, Stream.of(KEY).collect(Collectors.toList()));
        assertFalse(segmentImplementation.contains(FAKE_KEY));
    }

    @Test
    public void testChangeNumber(){
        SegmentImplementation segmentImplementation = new SegmentImplementation(CHANGE_NUMBER, SEGMENT_NAME);
        assertEquals(CHANGE_NUMBER, segmentImplementation.changeNumber());
    }

    @Test
    public void testSetChangeNumber(){
        SegmentImplementation segmentImplementation = new SegmentImplementation(CHANGE_NUMBER, SEGMENT_NAME);
        segmentImplementation.setChangeNumber(NEW_CHANGE_NUMBER);
        assertEquals(NEW_CHANGE_NUMBER, segmentImplementation.changeNumber());
    }

    @Test
    public void testUpdateSegment(){
        SegmentImplementation segmentImplementation = new SegmentImplementation(CHANGE_NUMBER, SEGMENT_NAME);
        segmentImplementation.updateSegment(Stream.of(KEY).collect(Collectors.toList()), new ArrayList<>());
        assertTrue(segmentImplementation.contains(KEY));
    }

    //this test is just to coverage. Force Refresh is not override in this implementation
    @Test
    public void testForceRefresh(){
        SegmentImplementation segmentImplementation = new SegmentImplementation(CHANGE_NUMBER, SEGMENT_NAME);
        segmentImplementation.forceRefresh();
    }
}