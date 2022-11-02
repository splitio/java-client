package redis;

import org.junit.Assert;
import org.junit.Test;
import pluggable.CustomStorageWrapper;
import redis.clients.jedis.JedisPool;
import redis.common.CommonRedis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RedisSingleTest {
    private final CommonRedis _commonRedis = CommonRedis.create("test-prefix:");

    @Test
    public void testSetAndGet() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("test-5", "5");
        map.put("test-6", "6");
        map.put("test-7", "7");
        map.put("test-8", "8");

        CustomStorageWrapper storageWrapper = new RedisSingle(new JedisPool(), "test-prefix:");

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

        CustomStorageWrapper storageWrapper = new RedisSingle(new JedisPool(), "test-prefix:");

        for (Map.Entry<String, String> entry : map.entrySet()) {
            storageWrapper.set(entry.getKey(), entry.getValue());
        }

        ArrayList<String> keys = new ArrayList<>();
        keys.add("test-5");
        keys.add("test-6");
        keys.add("test-10");
        List<String> expectedResult = Stream.of("5", "6", null).collect(Collectors.toList());
        List<String> result = storageWrapper.getMany(keys);

        Assert.assertEquals(expectedResult, result);

        storageWrapper.delete(new ArrayList<>(map.keySet()));
    }

    @Test
    public void testGetSet() throws Exception {
        String key = "test-getSet";
        Map<String, String> map = new HashMap<>();
        map.put(key, "5");

        CustomStorageWrapper storageWrapper = new RedisSingle(new JedisPool(), "test-prefix:");
        storageWrapper.set(key, "5");
        String result = storageWrapper.getAndSet(key, "7");
        Assert.assertEquals("5", result);
        Assert.assertEquals("7", storageWrapper.get(key));

        storageWrapper.delete(new ArrayList<>(map.keySet()));
    }

    @Test
    public void testGetKeysByPrefix() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("item-1", "1");
        map.put("item-2", "2");
        map.put("item-3", "3");
        map.put("i-4", "4");
        RedisSingle storageWrapper = new RedisSingle(new JedisPool(), "test-prefix:");
        try {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                storageWrapper.set(entry.getKey(), entry.getValue());
            }

            Set<String> result = storageWrapper.getKeysByPrefix("item*");

            Assert.assertEquals(3, result.size());
            Assert.assertTrue(result.contains("item-1"));
            Assert.assertTrue(result.contains("item-2"));
            Assert.assertTrue(result.contains("item-3"));
        }
        finally {
            storageWrapper.delete(new ArrayList<>(map.keySet()));
        }
    }

    @Test
    public void testIncrementAndDecrement() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("item-1", "2");
        RedisSingle storageWrapper = new RedisSingle(new JedisPool(), "test-prefix:");
        try {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                storageWrapper.set(entry.getKey(), entry.getValue());
            }

            long result = storageWrapper.increment("item-1", 2L);
            Assert.assertEquals(4L, result);

            result = storageWrapper.decrement("item-1", 3L);
            Assert.assertEquals(1L, result);
        }
        finally {
            storageWrapper.delete(new ArrayList<>(map.keySet()));
        }
    }

    @Test
    public void testHIncrement() throws Exception {
        RedisSingle storageWrapper = new RedisSingle(new JedisPool(), "test-prefix");
        Map<String, String> map = new HashMap<>();
        map.put("count", "test::12232");
        try {
            long result = storageWrapper.hIncrement("count", "test::12232", 2L);
            Assert.assertEquals(2L, result);
            result = storageWrapper.hIncrement("count", "test::12232", 1L);
            Assert.assertEquals(3L, result);
        } finally {
            storageWrapper.delete(new ArrayList<>(map.keySet()));
        }
    }

    @Test
    public void testPushAndPopItems() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("item-1", "1");
        RedisSingle storageWrapper = new RedisSingle(new JedisPool(), "test-prefix");
        try {
            long push = storageWrapper.pushItems("item-1", Arrays.asList("1", "2", "3", "4"));
            Assert.assertEquals(4L, push);

            List<String> result = storageWrapper.popItems("item-1", 3);
            Assert.assertEquals(3L, result.size());

            push = storageWrapper.pushItems("item-1", Arrays.asList("5"));
            Assert.assertEquals(2L, push);

        }
        finally {
            storageWrapper.delete(new ArrayList<>(map.keySet()));
        }
    }

    @Test
    public void testGetItemsCount() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("item-1", "1");
        RedisSingle storageWrapper = new RedisSingle(new JedisPool(), "test-prefix");
        try {
            storageWrapper.addItems("item-1", Arrays.asList("1", "2", "3", "4"));
            long result = storageWrapper.getItemsCount("item-1");

            Assert.assertEquals(4L, result);
        }
        finally {
            storageWrapper.delete(new ArrayList<>(map.keySet()));
        }
    }

    @Test
    public void testItemContains() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("item-1", "1");
        RedisSingle storageWrapper = new RedisSingle(new JedisPool(), "test-prefix");
        try {
            storageWrapper.addItems("item-1", Arrays.asList("1", "2", "3", "4"));
            boolean result = storageWrapper.itemContains("item-1", "2");

            Assert.assertTrue(result);
        }
        finally {
            storageWrapper.delete(new ArrayList<>(map.keySet()));
        }
    }

    @Test
    public void testRemoveItems() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("item-1", "1");
        RedisSingle storageWrapper = new RedisSingle(new JedisPool(), "test-prefix");
        try {
            storageWrapper.addItems("item-1", Arrays.asList("1", "2", "3", "4"));
            boolean result = storageWrapper.itemContains("item-1", "2");
            Assert.assertTrue(result);

            storageWrapper.removeItems("item-1", Arrays.asList("2", "4"));
            result = storageWrapper.itemContains("item-1", "2");
            Assert.assertFalse(result);
            result = storageWrapper.itemContains("item-1", "4");
            Assert.assertFalse(result);
        }
        finally {
            storageWrapper.delete(new ArrayList<>(map.keySet()));
        }
    }

    @Test
    public void testGetItems() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("item-1", "1");
        map.put("item-2", "2");
        map.put("item-3", "3");
        map.put("i-4", "4");
        RedisSingle storageWrapper = new RedisSingle(new JedisPool(), "test-prefix:");
        try {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                storageWrapper.set(entry.getKey(), entry.getValue());
            }

            Set<String> result = storageWrapper.getKeysByPrefix("item*");

            Assert.assertEquals(3, result.size());
            List<String> keys = new ArrayList<>();
            keys.add("item-1");
            keys.add("item-2");
            keys.add("item-3");
            List<String> items = storageWrapper.getItems(new ArrayList<>(keys));
            Assert.assertEquals(3, items.size());
            Assert.assertTrue(items.containsAll(Arrays.asList("1", "2", "3")));
        }
        finally {
            storageWrapper.delete(new ArrayList<>(map.keySet()));
        }
    }

    @Test
    public void testConnect() throws Exception {
        RedisSingle storageWrapper = new RedisSingle(new JedisPool(), "test-prefix");
        Assert.assertTrue(storageWrapper.connect());
    }

    @Test
    public void testDisconnect() throws Exception {
        RedisSingle storageWrapper = new RedisSingle(new JedisPool(), "test-prefix");
        Assert.assertTrue(storageWrapper.disconnect());
    }
}

