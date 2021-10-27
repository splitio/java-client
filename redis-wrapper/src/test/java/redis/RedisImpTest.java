package redis;

import org.junit.Assert;
import org.junit.Test;
import pluggable.CustomStorageWrapper;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedisImpTest {

    @Test
    public void testSetAndGet() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("test-5", "5");
        map.put("test-6", "6");
        map.put("test-7", "7");
        map.put("test-8", "8");

        CustomStorageWrapper storageWrapper = new RedisImp(new JedisPool(), "test-prefix:.");

        for (Map.Entry<String, String> entry : map.entrySet()) {
            storageWrapper.set(entry.getKey(), entry.getValue());
        }

        String result = storageWrapper.get("test-7");

        Assert.assertEquals("7", result);

        storageWrapper.delete(new ArrayList<>(map.keySet()));
    }

    @Test
    public void testSetAndGetMany() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("test-5", "5");
        map.put("test-6", "6");
        map.put("test-7", "7");
        map.put("test-8", "8");

        CustomStorageWrapper storageWrapper = new RedisImp(new JedisPool(), "test-prefix:.");

        for (Map.Entry<String, String> entry : map.entrySet()) {
            storageWrapper.set(entry.getKey(), entry.getValue());
        }

        ArrayList<String> keys = new ArrayList<>();
        keys.add("test-5");
        keys.add("test-6");
        keys.add("test-10");

        List<String> result = storageWrapper.getMany(keys);

        Assert.assertEquals("7", result);

        storageWrapper.delete(new ArrayList<>(map.keySet()));
    }
}

