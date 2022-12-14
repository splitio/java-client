package io.split.client.utils;

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
}