package redis;

import pluggable.Pipeline;
import redis.clients.jedis.JedisCluster;

import java.util.List;
import java.util.Set;

public class RedisCluster implements RedisUnified {
    private final JedisCluster jedis;

    public RedisCluster(JedisCluster jedisCluster) {
        this.jedis = jedisCluster;
    }

    @Override
    public String get(String key) throws Exception {
        try {
            return jedis.get(key);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public List<String> getMany(String[] keys) throws Exception {
        try {
            return jedis.mget(keys);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void set(String key, String item) throws Exception {
        try {
            jedis.set(key, item);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void hSet(String key, String field, String item) throws Exception {
        try {
            jedis.hset(key, field, item);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void delete(String[] keys) throws Exception {
        try {
            jedis.del(keys);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public String getAndSet(String key, String item) throws Exception {
        try {
            return jedis.getSet(key, item);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public Set<String> getKeysByPrefix(String prefix) throws Exception {
        try {
            return jedis.keys(prefix);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public long increment(String key, long value) throws Exception {
        try {
            return jedis.incrBy(key, value);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public long hIncrement(String key, String field, long value) throws RedisException {
        try {
            return jedis.hincrBy(key, field, value);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public long decrement(String key, long value) throws Exception {
        try {
            return jedis.decrBy(key, value);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public long pushItems(String key, String[] items) throws Exception {
        try {
            return jedis.rpush(key, items);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public List<String> popItems(String key, long count) throws Exception {
        try {
            List<String> items = jedis.lrange(key, 0, count-1);
            int fetchedCount = items.size();
            jedis.ltrim(key, fetchedCount, -1);
            return items;
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public long getItemsCount(String key) throws Exception {
        try {
            return jedis.scard(key);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public boolean itemContains(String key, String item) throws Exception {
        try {
            return jedis.sismember(key, item);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void addItems(String key, String[] items) throws Exception {
        try {
            jedis.sadd(key, items);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void removeItems(String key, String[] items) throws Exception {
        try {
            jedis.srem(key, items);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public List<String> getItems(String[] keys) throws Exception {
        try {
            return jedis.mget(keys);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public boolean connect() throws Exception {
        try {
            return "PONG".equalsIgnoreCase(jedis.echo("PONG"));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public boolean disconnect() throws Exception {
        try {
            jedis.close();

            return true;
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void expire(String key, long ttl) throws Exception {
        try {
            jedis.pexpire(key, ttl);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public Pipeline pipeline() throws Exception {
        try {
            // return new RedisPipeline(this.jedisPool, this._prefix);
            return null;
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }
}
