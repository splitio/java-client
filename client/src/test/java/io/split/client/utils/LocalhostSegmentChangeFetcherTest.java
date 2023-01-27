package io.split.client.utils;

import io.split.client.LocalhostSegmentChangeFetcher;
import io.split.client.dtos.SegmentChange;
import io.split.engine.common.FetchOptions;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class LocalhostSegmentChangeFetcherTest {

    @Test
    public void testSegmentFetch(){
        LocalhostSegmentChangeFetcher localhostSegmentChangeFetcher = new LocalhostSegmentChangeFetcher("src/test/resources/");
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SegmentChange segmentChange = localhostSegmentChangeFetcher.fetch("segment_1", -1L, fetchOptions);

        Assert.assertEquals("segment_1", segmentChange.name);
        Assert.assertEquals(4, segmentChange.added.size());
    }

    @Test
    public void testSegmentNameNull(){
        LocalhostSegmentChangeFetcher localhostSegmentChangeFetcher = new LocalhostSegmentChangeFetcher("src/test/resources/sanitizer/");
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SegmentChange segmentChange = localhostSegmentChangeFetcher.fetch("segmentNameNull", -1L, fetchOptions);

        Assert.assertNull(segmentChange);
    }

    @Test
    public void sameInAddedAndRemoved(){
        LocalhostSegmentChangeFetcher localhostSegmentChangeFetcher = new LocalhostSegmentChangeFetcher("src/test/resources/sanitizer/");
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SegmentChange segmentChange = localhostSegmentChangeFetcher.fetch("sameInAddedAndRemoved", -1L, fetchOptions);

        Assert.assertEquals(0, segmentChange.removed.size());
        Assert.assertEquals(4, segmentChange.added.size());
    }

    @Test
    public void checkTillAndSince(){
        LocalhostSegmentChangeFetcher localhostSegmentChangeFetcher = new LocalhostSegmentChangeFetcher("src/test/resources/sanitizer/");
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SegmentChange segmentChange = localhostSegmentChangeFetcher.fetch("segmentChangeSinceTill", -1L, fetchOptions);

        Assert.assertEquals(-1L, segmentChange.till);
        Assert.assertEquals(-1L, segmentChange.since);
    }
}