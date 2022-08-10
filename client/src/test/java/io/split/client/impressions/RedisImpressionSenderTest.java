package io.split.client.impressions;

import io.split.storages.pluggable.domain.userStorageWrapper;
import org.junit.Test;
import org.mockito.Mockito;
import pluggable.CustomStorageWrapper;

import java.lang.reflect.Field;
import java.util.HashMap;

public class RedisImpressionSenderTest {

    @Test
    public void testPostCounters() throws NoSuchFieldException, IllegalAccessException {
        userStorageWrapper userStorageWrapper = Mockito.mock(userStorageWrapper.class);
        RedisImpressionSender redisImpressionSender = RedisImpressionSender.create(Mockito.mock(CustomStorageWrapper.class));
        Field redisSubmitterHolder = RedisImpressionSender.class.getDeclaredField("_userStorageWrapper");
        redisSubmitterHolder.setAccessible(true);

        redisSubmitterHolder.set(redisImpressionSender, userStorageWrapper);

        HashMap<ImpressionCounter.Key, Integer> counters =  new HashMap<>();
        ImpressionCounter.Key counterKey1 =  new ImpressionCounter.Key("feature1", 100);
        counters.put(counterKey1,2);
        redisImpressionSender.postCounters(counters);
        Mockito.verify(userStorageWrapper, Mockito.times(1)).hIncrement(Mockito.eq("SPLITIO.impressions.count"), Mockito.eq("feature1::100"), Mockito.eq(2L));
    }
}