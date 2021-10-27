package redis;

import pluggable.CustomStorageWrapper;
import redis.clients.jedis.JedisPool;

public class RedisInstance {
    public static CustomStorageWrapper getRedisInstance(String url, int port, String prefix) {
        JedisPool jedisPool = new JedisPool(url, port);
        return new RedisImp(jedisPool, prefix);
    }
}
