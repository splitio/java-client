package io.split.client.impressions;

import org.junit.Test;
import org.mockito.Mockito;
import pluggable.CustomStorageWrapper;

import java.util.HashMap;

public class RedisImpressionSenderTest {

    @Test
    public void testPostCounters() throws Exception {
        CustomStorageWrapper customStorageWrapper = Mockito.mock(CustomStorageWrapper.class);
        RedisImpressionSender redisImpressionSender = RedisImpressionSender.create(customStorageWrapper);

        HashMap<ImpressionCounter.Key, Integer> counters =  new HashMap<>();
        ImpressionCounter.Key counterKey1 =  new ImpressionCounter.Key("feature1", 100);
        counters.put(counterKey1,2);
        redisImpressionSender.postCounters(counters);
        Mockito.verify(customStorageWrapper, Mockito.times(1)).hIncrement(Mockito.eq("SPLITIO.impressions.count"), Mockito.eq("feature1::100"), Mockito.eq(2L));
    }
}