package io.split.client;

import com.google.common.collect.Maps;
import io.split.client.dtos.SplitChange;
import io.split.client.utils.LocalhostUtils;
import io.split.engine.common.FetchOptions;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class LegacyLocalhostSplitChangeFetcherTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testParseSplitChange() throws IOException {
        File file = folder.newFile(LegacyLocalhostSplitChangeFetcher.FILENAME);

        Map<SplitAndKey, LocalhostSplit> map = Maps.newHashMap();
        map.put(SplitAndKey.of("onboarding"), LocalhostSplit.of("on"));
        map.put(SplitAndKey.of("onboarding", "user1"), LocalhostSplit.of("off"));
        map.put(SplitAndKey.of("onboarding", "user2"), LocalhostSplit.of("off"));
        map.put(SplitAndKey.of("test"), LocalhostSplit.of("a"));

        LocalhostUtils.writeFile(file, map);

        LegacyLocalhostSplitChangeFetcher localhostSplitChangeFetcher = new LegacyLocalhostSplitChangeFetcher(folder.getRoot().getAbsolutePath());
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);
        SplitChange splitChange = localhostSplitChangeFetcher.fetch(-1L, -1, fetchOptions);

        Assert.assertEquals(2, splitChange.featureFlags.d.size());
        Assert.assertEquals(-1, splitChange.featureFlags.s);
        Assert.assertEquals(-1, splitChange.featureFlags.t);
    }
}