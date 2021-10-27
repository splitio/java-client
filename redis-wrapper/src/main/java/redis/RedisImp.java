package redis;

import pluggable.CustomStorageWrapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RedisImp implements CustomStorageWrapper {

    private static final String SPLIT_KEY = "SPLITIO.split.*";
    private final JedisPool redisPool;
    private Jedis jedis;
    private String _prefix;

    public RedisImp(JedisPool jedisPool, String prefix) {
        redisPool = jedisPool;
        jedis = redisPool.getResource();
        _prefix = prefix;
    }

    @Override
    public String get(String s) throws Exception {
        return null;
    }

    @Override
    public List<String> getMany(List<String> list) throws Exception {
        List<String> keys = new ArrayList<>(this.getKeysByPrefix(""));
        List<String> items = jedis.mget(keys.toArray(new String[keys.size()]));
        return items;
    }

    @Override
    public void set(String s, String s1) throws Exception {

    }

    @Override
    public void delete(List<String> list) throws Exception {

    }

    @Override
    public String getAndSet(String s, String s1) throws Exception {
        return null;
    }

    @Override
    public Set<String> getKeysByPrefix(String s) throws Exception {
        return jedis.keys(_prefix + SPLIT_KEY);
    }

    @Override
    public long increment(String s, long l) throws Exception {
        return 0;
    }

    @Override
    public long decrement(String s, long l) throws Exception {
        return 0;
    }

    @Override
    public void pushItems(String s, List<String> list) throws Exception {

    }

    @Override
    public List<String> popItems(String s, long l) throws Exception {
        return null;
    }

    @Override
    public long getItemsCount(String s) throws Exception {
        return 0;
    }

    @Override
    public boolean itemContains(String s, String s1) throws Exception {
        return false;
    }

    @Override
    public void addItems(String s, List<String> list) throws Exception {

    }

    @Override
    public void removeItems(String s, List<String> list) throws Exception {

    }

    @Override
    public List<String> getItems(List<String> list) throws Exception {
        return null;
    }

    @Override
    public boolean connect() throws Exception {
        String ping = jedis.ping();
        return "PONG".equalsIgnoreCase(ping);
    }

    @Override
    public boolean close() throws Exception {
        return false;
    }
}

