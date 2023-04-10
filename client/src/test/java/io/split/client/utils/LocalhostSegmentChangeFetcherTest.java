package io.split.client.utils;

import io.split.client.LocalhostSegmentChangeFetcher;
import io.split.client.dtos.SegmentChange;
import io.split.engine.common.FetchOptions;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;

public class LocalhostSegmentChangeFetcherTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private String TEST_0 = "{\"name\":\"segment_test\",\"added\":[\"user-1\"],\"removed\":[],\"since\":-1,\"till\":-1}";
    private String TEST_1 = "{\"name\":\"segment_test\",\"added\":[\"user-1\"],\"removed\":[\"user-2\"],\"since\":-1,\"till\":-1}";
    private String TEST_2 = "{\"name\":\"segment_test\",\"added\":[\"user-1\"],\"removed\":[\"user-2\"],\"since\":-1,\"till\":2323}";
    private String TEST_3 = "{\"name\":\"segment_test\",\"added\":[\"user-1\",\"user-3\"],\"removed\":[\"user-2\"],\"since\":-1,\"till\":2323}";
    private String TEST_4 = "{\"name\":\"segment_test\",\"added\":[\"user-1\",\"user-3\"],\"removed\":[\"user-2\"],\"since\":-1,\"till\":445345}";
    private String TEST_5 = "{\"name\":\"segment_test\",\"added\":[\"user-1\"],\"removed\":[\"user-2\",\"user-3\"],\"since\":-1,\"till\":-1}";

    @Test
    public void testSegmentFetch() {
        LocalhostSegmentChangeFetcher localhostSegmentChangeFetcher = new LocalhostSegmentChangeFetcher("src/test/resources/");
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SegmentChange segmentChange = localhostSegmentChangeFetcher.fetch("segment_1", -1L, fetchOptions);

        Assert.assertEquals("segment_1", segmentChange.name);
        Assert.assertEquals(4, segmentChange.added.size());
    }

    @Test
    public void testSegmentNameNull() {
        LocalhostSegmentChangeFetcher localhostSegmentChangeFetcher = new LocalhostSegmentChangeFetcher("src/test/resources/sanitizer/");
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SegmentChange segmentChange = localhostSegmentChangeFetcher.fetch("segmentNameNull", -1L, fetchOptions);

        Assert.assertNull(segmentChange);
    }

    @Test
    public void sameInAddedAndRemoved() {
        LocalhostSegmentChangeFetcher localhostSegmentChangeFetcher = new LocalhostSegmentChangeFetcher("src/test/resources/sanitizer/");
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SegmentChange segmentChange = localhostSegmentChangeFetcher.fetch("sameInAddedAndRemoved", -1L, fetchOptions);

        Assert.assertEquals(0, segmentChange.removed.size());
        Assert.assertEquals(4, segmentChange.added.size());
    }

    @Test
    public void checkTillAndSince() {
        LocalhostSegmentChangeFetcher localhostSegmentChangeFetcher = new LocalhostSegmentChangeFetcher("src/test/resources/sanitizer/");
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SegmentChange segmentChange = localhostSegmentChangeFetcher.fetch("segmentChangeSinceTill", -1L, fetchOptions);

        Assert.assertEquals(-1L, segmentChange.till);
        Assert.assertEquals(-1L, segmentChange.since);
    }

    @Test
    public void testProcessSegmentFetch() throws IOException {
        File file = folder.newFile("segment_test.json");

        byte[] test = TEST_0.getBytes();
        com.google.common.io.Files.write(test, file);

        LocalhostSegmentChangeFetcher localhostSplitChangeFetcher = new LocalhostSegmentChangeFetcher(folder.getRoot().getAbsolutePath());
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        // 0) The CN from storage is -1, till and since are -1, and sha doesn't exist in the hash. It's going to return a segment change with updates.
        SegmentChange segmentChange = localhostSplitChangeFetcher.fetch("segment_test",-1L, fetchOptions);

        Assert.assertEquals(1, segmentChange.added.size());
        Assert.assertTrue(segmentChange.added.contains("user-1"));
        Assert.assertEquals(0, segmentChange.removed.size());
        Assert.assertEquals(-1, segmentChange.till);
        Assert.assertEquals(-1, segmentChange.since);

        test = TEST_1.getBytes();
        com.google.common.io.Files.write(test, file);

        // 1) The CN from storage is -1, till and since are -1, and sha is different than before. It's going to return a segment change with updates.
        segmentChange = localhostSplitChangeFetcher.fetch("segment_test",-1L, fetchOptions);
        Assert.assertEquals(1, segmentChange.added.size());
        Assert.assertTrue(segmentChange.added.contains("user-1"));
        Assert.assertEquals(1, segmentChange.removed.size());
        Assert.assertTrue(segmentChange.removed.contains("user-2"));
        Assert.assertEquals(-1, segmentChange.till);
        Assert.assertEquals(-1, segmentChange.since);

        test = TEST_2.getBytes();
        com.google.common.io.Files.write(test, file);

        // 2) The CN from storage is -1, till is 2323, and since is -1, and sha is the same as before. It's going to return a segment change with the same data.
        segmentChange = localhostSplitChangeFetcher.fetch("segment_test",-1L, fetchOptions);
        Assert.assertEquals(1, segmentChange.added.size());
        Assert.assertTrue(segmentChange.added.contains("user-1"));
        Assert.assertEquals(1, segmentChange.removed.size());
        Assert.assertTrue(segmentChange.removed.contains("user-2"));
        Assert.assertEquals(-1, segmentChange.till);
        Assert.assertEquals(-1, segmentChange.since);

        test = TEST_3.getBytes();
        com.google.common.io.Files.write(test, file);

        // 3) The CN from storage is -1, till is 2323, and since is -1, sha is different than before. It's going to return a segment change with updates.
        segmentChange = localhostSplitChangeFetcher.fetch("segment_test",-1L, fetchOptions);
        Assert.assertEquals(2, segmentChange.added.size());
        Assert.assertTrue(segmentChange.added.contains("user-1"));
        Assert.assertEquals(1, segmentChange.removed.size());
        Assert.assertTrue(segmentChange.removed.contains("user-2"));
        Assert.assertEquals(2323, segmentChange.till);
        Assert.assertEquals(-1, segmentChange.since);

        test = TEST_4.getBytes();
        com.google.common.io.Files.write(test, file);

        // 4) The CN from storage is 2323, till is 445345, and since is -1, and sha is the same as before. It's going to return a segment change with same data.
        segmentChange = localhostSplitChangeFetcher.fetch("segment_test",2323, fetchOptions);
        Assert.assertEquals(2, segmentChange.added.size());
        Assert.assertTrue(segmentChange.added.contains("user-1"));
        Assert.assertTrue(segmentChange.added.contains("user-3"));
        Assert.assertEquals(1, segmentChange.removed.size());
        Assert.assertTrue(segmentChange.removed.contains("user-2"));
        Assert.assertEquals(2323, segmentChange.till);
        Assert.assertEquals(2323, segmentChange.since);

        test = TEST_5.getBytes();
        com.google.common.io.Files.write(test, file);

        // 5) The CN from storage is 2323, till and since are -1, and sha is different than before. It's going to return a segment change with updates.
        segmentChange = localhostSplitChangeFetcher.fetch("segment_test",2323, fetchOptions);
        Assert.assertEquals(1, segmentChange.added.size());
        Assert.assertTrue(segmentChange.added.contains("user-1"));
        Assert.assertFalse(segmentChange.added.contains("user-3"));
        Assert.assertEquals(2, segmentChange.removed.size());
        Assert.assertTrue(segmentChange.removed.contains("user-2"));
        Assert.assertTrue(segmentChange.removed.contains("user-3"));
        Assert.assertEquals(2323, segmentChange.till);
        Assert.assertEquals(2323, segmentChange.since);
    }
}