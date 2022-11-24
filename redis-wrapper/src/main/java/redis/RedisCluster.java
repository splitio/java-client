package redis;

import pluggable.CustomStorageWrapper;
import pluggable.Pipeline;
import redis.clients.jedis.JedisCluster;
import redis.common.CommonRedis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class RedisCluster implements CustomStorageWrapper {
    private final CommonRedis _commonRedis;
    private final JedisCluster jedis;

    public static final String DEFAULT_HASHTAG = "{SPLITIO}" ;

    private String validateHashtag(String hashtag) {
        if (hashtag == null) {
            return DEFAULT_HASHTAG;
        }
        if (hashtag.length() <= 2) {
            return DEFAULT_HASHTAG;
        }
        if (!hashtag.startsWith("{")) {
            return DEFAULT_HASHTAG;
        }
        if (!hashtag.endsWith("}")) {
            return DEFAULT_HASHTAG;
        }

        return hashtag;
    }

    public RedisCluster(JedisCluster jedisCluster, String prefix, String hashtag) {
        this.jedis = jedisCluster;
        _commonRedis = CommonRedis.create(validateHashtag(hashtag) + prefix);
    }

    @Override
    public String get(String key) throws Exception {
        try {
            return jedis.get(_commonRedis.buildKeyWithPrefix(key));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public List<String> getMany(List<String> keys) throws Exception {
        if(keys == null || keys.isEmpty()){
            return new ArrayList<>();
        }
        try {
            keys = keys.stream().map(key -> _commonRedis.buildKeyWithPrefix(key)).collect(Collectors.toList());

            return jedis.mget(keys.toArray(new String[keys.size()]));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void set(String key, String item) throws Exception {
        try {
            if(key.contains(_commonRedis.TELEMETRY_INIT)) {
                String[] splittedKey = key.split("::");
                jedis.hset(_commonRedis.buildKeyWithPrefix(splittedKey[0]), splittedKey[1], item);
                return;
            }
            jedis.set(_commonRedis.buildKeyWithPrefix(key), item);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void hSet(String key, String field, String item) throws Exception {
        try {
            jedis.hset(_commonRedis.buildKeyWithPrefix(key), field, item);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void delete(List<String> keys) throws Exception {
        if(keys == null || keys.isEmpty()){
            return ;
        }
        try {
            keys = keys.stream().map(key -> _commonRedis.buildKeyWithPrefix(key)).collect(Collectors.toList());

            jedis.del(keys.toArray(new String[keys.size()]));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public String getAndSet(String key, String item) throws Exception {
        //Todo if this method isn't used we should deprecated
        try {
            return jedis.getSet(_commonRedis.buildKeyWithPrefix(key), item);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public Set<String> getKeysByPrefix(String prefix) throws Exception {
        try {
            Set<String> keysWithPrefix = jedis.keys(_commonRedis.buildKeyWithPrefix(prefix));
            keysWithPrefix = keysWithPrefix.stream().map(key -> key.replace(_commonRedis.getPrefix() + ".", "")).collect(Collectors.toSet());
            return keysWithPrefix;
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public long increment(String key, long value) throws Exception {
        try {
            return jedis.incrBy(_commonRedis.buildKeyWithPrefix(key), value);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public long hIncrement(String key, String field, long value) throws RedisException {
        try {
            return jedis.hincrBy(_commonRedis.buildKeyWithPrefix(key), field, value);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public long decrement(String key, long value) throws Exception {
        try {
            return jedis.decrBy(_commonRedis.buildKeyWithPrefix(key), value);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public long pushItems(String key, List<String> items) throws Exception {
        try {
            long addedItems = jedis.rpush(_commonRedis.buildKeyWithPrefix(key), items.toArray(new String[items.size()]));
            if(_commonRedis.EVENTS_KEY.equals(key) || _commonRedis.IMPRESSIONS_KEY.equals(key)) {
                if(addedItems == items.size()) {
                    jedis.pexpire(key, _commonRedis.IMPRESSIONS_OR_EVENTS_DEFAULT_TTL);
                }
            }
            return addedItems;
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public List<String> popItems(String key, long count) throws Exception {
        try {
            String keyWithPrefix = _commonRedis.buildKeyWithPrefix(key);
            List<String> items = jedis.lrange(keyWithPrefix, 0, count-1);
            int fetchedCount = items.size();
            jedis.ltrim(keyWithPrefix, fetchedCount, -1);
            return items;
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    // Return length of redis set.
    @Override
    public long getItemsCount(String key) throws Exception {
        try {
            return jedis.scard(_commonRedis.buildKeyWithPrefix(key));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public boolean itemContains(String key, String item) throws Exception {
        try {
            return jedis.sismember(_commonRedis.buildKeyWithPrefix(key), item);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void addItems(String key, List<String> items) throws Exception {
        try {
            jedis.sadd(_commonRedis.buildKeyWithPrefix(key), items.toArray(new String[items.size()]));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void removeItems(String key, List<String> items) throws Exception {
        try {
            jedis.srem(_commonRedis.buildKeyWithPrefix(key), items.toArray(new String[items.size()]));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public List<String> getItems(List<String> keys) throws Exception {
        if(keys == null || keys.isEmpty()){
            return new ArrayList<>();
        }
        try {
            keys = keys.stream().map(key -> _commonRedis.buildKeyWithPrefix(key)).collect(Collectors.toList());

            return jedis.mget(keys.toArray(new String[keys.size()]));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public boolean connect() throws Exception {
        try {
            return jedis.getClusterNodes().entrySet().stream().findFirst().map(e -> e.getValue().getResource().isConnected()).orElse(false);
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
    public Pipeline pipeline() throws Exception {
        return null;
    }
}