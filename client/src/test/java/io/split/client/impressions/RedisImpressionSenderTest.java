package io.split.client.impressions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;

public class RedisImpressionSenderTest {

    private RedisImpressionSender _redisImpressionSender;
    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        _redisImpressionSender = Mockito.mock(RedisImpressionSender.class);
    }

    @Test
    public void testPostCounters(){
        HashMap<ImpressionCounter.Key, Integer> counters =  new HashMap<>();
        ImpressionCounter.Key counterKey1 =  new ImpressionCounter.Key("feature1", 100);
        counters.put(counterKey1,2);
        ImpressionCounter.Key counterKey2 = new ImpressionCounter.Key("feature2", 200);
        counters.put(counterKey2, 1);
        _redisImpressionSender.postCounters(counters);
        Mockito.verify(_redisImpressionSender, Mockito.times(1)).postCounters(counters);
    }
}