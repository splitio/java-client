package io.split.client.impressions;

import io.split.storages.pluggable.domain.SafeUserStorageWrapper;
import org.junit.Test;
import org.mockito.Mockito;
import pluggable.CustomStorageWrapper;

import java.lang.reflect.Field;
import java.util.HashMap;

public class RedisImpressionSenderTest {

    @Test
    public void testPostCounters() throws NoSuchFieldException, IllegalAccessException {
        SafeUserStorageWrapper safeUserStorageWrapper = Mockito.mock(SafeUserStorageWrapper.class);
        RedisImpressionSender redisImpressionSender = RedisImpressionSender.create(Mockito.mock(CustomStorageWrapper.class));
        Field redisSubmitterHolder = RedisImpressionSender.class.getDeclaredField("_safeUserStorageWrapper");
        redisSubmitterHolder.setAccessible(true);

        redisSubmitterHolder.set(redisImpressionSender, safeUserStorageWrapper);

        HashMap<ImpressionCounter.Key, Integer> counters =  new HashMap<>();
        ImpressionCounter.Key counterKey1 =  new ImpressionCounter.Key("feature1", 100);
        counters.put(counterKey1,2);
        redisImpressionSender.postCounters(counters);
        Mockito.verify(safeUserStorageWrapper, Mockito.times(1)).increment(Mockito.eq("SPLITIO.impressions.count.feature1::100"), Mockito.anyLong());
    }
}