package io.split.engine.experiments;

import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import io.split.engine.common.FetchOptions;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

public class LocalhostSplitChangeFetcherTest {

    @Test
    public void testParseSplitChange(){
        LocalhostSplitChangeFetcher localhostSplitChangeFetcher = new LocalhostSplitChangeFetcher("src/test/resources/split_init.json");
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SplitChange splitChange = localhostSplitChangeFetcher.fetch(-1L, fetchOptions);

        List<Split> split = splitChange.splits;
        Assert.assertEquals(7, split.size());
        Assert.assertEquals(1660326991072L, splitChange.till);
        Assert.assertEquals(-1, splitChange.since);
    }
}