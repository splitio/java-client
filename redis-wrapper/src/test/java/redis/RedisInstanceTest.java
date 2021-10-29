package redis;

import org.junit.Assert;
import org.junit.Test;
import pluggable.CustomStorageWrapper;
import redis.clients.jedis.JedisPool;

public class RedisInstanceTest {

    @Test
    public void testRedisInstanceBuilder() {
        CustomStorageWrapper redisInstance = RedisInstance.builder()
                .host("localhost")
                .timeout(1500)
                .database(0)
                .build();
        Assert.assertNotNull(redisInstance);
    }

    @Test
    public void testRedisInstanceBuilderWithJedisPool() {
        JedisPool jedisPool = new JedisPool();
        CustomStorageWrapper redisInstance = RedisInstance.builder()
                .jedisPool(jedisPool)
                .prefix("test")
                .build();
        Assert.assertNotNull(redisInstance);
    }

}