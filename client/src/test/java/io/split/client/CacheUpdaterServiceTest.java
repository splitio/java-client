package io.split.client;

import io.split.client.interceptors.FlagSetsFilterImpl;
import io.split.storages.memory.InMemoryCacheImp;
import io.split.storages.SplitCache;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class CacheUpdaterServiceTest {

    private static final String OFF_TREATMENT = "off";
    private static final String ON_TREATMENT = "on";
    private static final String MY_FEATURE = "my_feature";
    private SplitClientConfig config = SplitClientConfig.builder().setBlockUntilReadyTimeout(100).build();

    @Test
    public void testCacheUpdate() {
        SplitCache splitCache = new InMemoryCacheImp(new FlagSetsFilterImpl(new HashSet<>()));
        CacheUpdaterService cacheUpdaterService = new CacheUpdaterService(splitCache);
        cacheUpdaterService.updateCache(getMap());
        Assert.assertNotNull(splitCache.get(MY_FEATURE));
    }

    public Map<SplitAndKey, LocalhostSplit> getMap() {
        Map<SplitAndKey, LocalhostSplit> map = new HashMap<>();
        SplitAndKey splitAndKey = new SplitAndKey(MY_FEATURE, "onley_key");
        LocalhostSplit split = new LocalhostSplit(OFF_TREATMENT, "{\\\"desc\\\" : \\\"this applies only to OFF and only for only_key. The rest will receive ON\\\"}");
        map.put(splitAndKey, split);
        splitAndKey = new SplitAndKey("other_feature_2", null);
        split = new LocalhostSplit(ON_TREATMENT, null);
        map.put(splitAndKey, split);
        splitAndKey = new SplitAndKey("other_feature_3", null);
        split = new LocalhostSplit(OFF_TREATMENT, null);
        map.put(splitAndKey, split);
        splitAndKey = new SplitAndKey(MY_FEATURE, "key");
        split = new LocalhostSplit(ON_TREATMENT, "{\\\"desc\\\" : \\\"this applies only to ON treatment\\\"}");
        map.put(splitAndKey, split);
        splitAndKey = new SplitAndKey("other_feature_3", "key_whitelist");
        split = new LocalhostSplit(ON_TREATMENT, null);
        map.put(splitAndKey, split);
        return map;
    }
}