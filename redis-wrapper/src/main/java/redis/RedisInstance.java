package redis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import pluggable.CustomStorageWrapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisInstance {

    public static CustomStorageWrapper getRedisInstance(String host, int port, int timeout, String user, String password, int database, String prefix) {
        GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();
        JedisPool jedisPool = new JedisPool(poolConfig, host, port, timeout, user, password, database);
        return new RedisImp(jedisPool, prefix);
    }

    public static CustomStorageWrapper getRedisInstance(JedisPool jedisPool, String prefix) {
        return new RedisImp(jedisPool, prefix);
    }
}
