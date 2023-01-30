package io.split.client;

import io.split.client.dtos.ConditionType;
import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import io.split.client.dtos.Status;
import io.split.engine.common.FetchOptions;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

public class LocalhostSplitChangeFetcherTest {

    @Test
    public void testParseSplitChange(){
        LocalhostSplitChangeFetcher localhostSplitChangeFetcher = new LocalhostSplitChangeFetcher("src/test/resources/split_init.json");
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SplitChange splitChange = localhostSplitChangeFetcher.fetch(-1L, fetchOptions);

        List<Split> split = splitChange.splits;
        Assert.assertEquals(7, split.size());
        Assert.assertEquals(1660326991072L, splitChange.till);
        Assert.assertEquals(-1L, splitChange.since);
    }

    @Test
    public void testSinceAndTillSanitization(){
        LocalhostSplitChangeFetcher localhostSplitChangeFetcher = new LocalhostSplitChangeFetcher("src/test/resources/sanitizer/splitChangeTillSanitization.json");
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SplitChange splitChange = localhostSplitChangeFetcher.fetch(-1L, fetchOptions);

        Assert.assertEquals(-1L, splitChange.till);
        Assert.assertEquals(-1L, splitChange.since);
    }

    @Test
    public void testSplitChangeWithoutSplits(){
        LocalhostSplitChangeFetcher localhostSplitChangeFetcher = new LocalhostSplitChangeFetcher("src/test/resources/sanitizer/splitChangeWithoutSplits.json");
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SplitChange splitChange = localhostSplitChangeFetcher.fetch(-1L, fetchOptions);

        Assert.assertEquals(0, splitChange.splits.size());
    }

    @Test
    public void testSplitChangeSplitsToSanitize(){
        LocalhostSplitChangeFetcher localhostSplitChangeFetcher = new LocalhostSplitChangeFetcher("src/test/resources/sanitizer/splitChangeSplitsToSanitize.json");
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SplitChange splitChange = localhostSplitChangeFetcher.fetch(-1L, fetchOptions);

        Assert.assertEquals(1, splitChange.splits.size());
        Split split = splitChange.splits.get(0);
        Assert.assertEquals(Optional.of(100), Optional.of(split.trafficAllocation));
        Assert.assertEquals(Status.ACTIVE, split.status);
        Assert.assertEquals("on", split.defaultTreatment);
        Assert.assertEquals(ConditionType.ROLLOUT, split.conditions.get(split.conditions.size() - 1).conditionType);
    }

    @Test
    public void testSplitChangeSplitsToSanitizeMatchersNull(){
        LocalhostSplitChangeFetcher localhostSplitChangeFetcher = new LocalhostSplitChangeFetcher("src/test/resources/sanitizer/splitChangerMatchersNull.json");
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SplitChange splitChange = localhostSplitChangeFetcher.fetch(-1L, fetchOptions);

        Assert.assertEquals(1, splitChange.splits.size());
        Split split = splitChange.splits.get(0);
        Assert.assertEquals(Optional.of(100), Optional.of(split.trafficAllocation));
        Assert.assertEquals(Status.ACTIVE, split.status);
        Assert.assertEquals("off", split.defaultTreatment);
        Assert.assertEquals(ConditionType.ROLLOUT, split.conditions.get(split.conditions.size() - 1).conditionType);
    }
}