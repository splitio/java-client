package io.split.client;

import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import io.split.engine.common.FetchOptions;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class YamlResourceLocalhostSplitChangeFetcherTest {

    @Test
    public void testParseSplitChange() {
        YamlLocalhostSplitChangeFetcher yamlLocalhostSplitChangeFetcher = new YamlResourceLocalhostSplitChangeFetcher("split.yaml");

        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);
        SplitChange splitChange = yamlLocalhostSplitChangeFetcher.fetch(-1L, fetchOptions);

        Assert.assertEquals(4, splitChange.splits.size());
        Assert.assertEquals(-1, splitChange.since);
        Assert.assertEquals(-1, splitChange.till);

        for (Split split: splitChange.splits) {
            Assert.assertEquals("control", split.defaultTreatment);
        }
    }
}