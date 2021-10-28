package redis;

import pluggable.CustomStorageWrapper;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisInstance {

    public static CustomStorageWrapper getRedisInstance(String host, int port, int timeout, String user, String password, int database, String prefix) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        JedisPool jedisPool = new JedisPool(poolConfig, host, port, timeout, password, database);
        return new RedisImp(jedisPool, prefix);
    }

    public static CustomStorageWrapper getRedisInstance(JedisPool jedisPool, String prefix) {
        return new RedisImp(jedisPool, prefix);
    }
}
