package io.split.client.impressions;

import io.split.storages.pluggable.CustomStorageWrapperHasPipeline;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import pluggable.CustomStorageWrapper;
import pluggable.HasPipelineSupport;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

public class RedisImpressionSenderTest {

    @Test
    public void testPostCounters() throws Exception {
        CustomStorageWrapper customStorageWrapper = Mockito.mock(CustomStorageWrapper.class);
        RedisImpressionSender redisImpressionSender = RedisImpressionSender.create(customStorageWrapper);

        HashMap<ImpressionCounter.Key, Integer> counters =  new HashMap<>();
        ImpressionCounter.Key counterKey1 =  new ImpressionCounter.Key("feature1", 100);
        counters.put(counterKey1, 2);
        redisImpressionSender.postCounters(counters);
        Mockito.verify(customStorageWrapper, Mockito.times(1)).hIncrement(Mockito.eq("SPLITIO.impressions.count"), Mockito.eq("feature1::100"), Mockito.eq(2L));
    }

    @Test
    public void testPostCountersHasPipeline() throws Exception {
        CustomStorageWrapperHasPipeline customStorageWrapper = new CustomStorageWrapperHasPipeline();
        RedisImpressionSender redisImpressionSender = RedisImpressionSender.create(customStorageWrapper);

        HashMap<ImpressionCounter.Key, Integer> counters =  new HashMap<>();
        ImpressionCounter.Key counterKey1 =  new ImpressionCounter.Key("feature1", 100);
        counters.put(counterKey1, 2);
        redisImpressionSender.postCounters(counters);

        Assert.assertTrue(customStorageWrapper instanceof HasPipelineSupport);
        ConcurrentMap<String, Long> impressionsCount = customStorageWrapper.getImpressionsCount();
        Assert.assertTrue(impressionsCount.containsKey("feature1::100"));
        String key = "feature1::100";
        Assert.assertEquals(Optional.of(2L), Optional.of(impressionsCount.get(key)));
    }
}