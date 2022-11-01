package redis;

import pluggable.CustomStorageWrapper;
import pluggable.HasPipelineSupport;
import pluggable.Pipeline;
import redis.common.CommonRedis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class RedisImp implements CustomStorageWrapper, HasPipelineSupport {
    private static final String TELEMETRY_INIT = "SPLITIO.telemetry.init" ;
    private static final String EVENTS_KEY = "SPLITIO.events" ;
    private static final String IMPRESSIONS_KEY = "SPLITIO.impressions" ;
    private static final long IMPRESSIONS_OR_EVENTS_DEFAULT_TTL = 3600000L;

    private final RedisUnified redisUnified;
    private final CommonRedis _commonRedis;
    private final String _prefix;

    public RedisImp(RedisUnified redisUnified, String prefix) {
        this.redisUnified = redisUnified;
        this._prefix = prefix;
        _commonRedis = CommonRedis.create(prefix);
    }

    @Override
    public String get(String key) throws Exception {
        try {
            return redisUnified.get(_commonRedis.buildKeyWithPrefix(key));
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

            return redisUnified.getMany(keys.toArray(new String[keys.size()]));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void set(String key, String item) throws Exception {
        try {
            if(key.contains(TELEMETRY_INIT)) {
                String[] splittedKey = key.split("::");
                redisUnified.hSet(_commonRedis.buildKeyWithPrefix(splittedKey[0]), splittedKey[1], item);
                return;
            }
            redisUnified.set(_commonRedis.buildKeyWithPrefix(key), item);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void hSet(String key, String field, String item) throws Exception {
        try {
            redisUnified.hSet(_commonRedis.buildKeyWithPrefix(key), field, item);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void delete(List<String> keys) throws Exception {
        if(keys == null || keys.isEmpty()){
            return;
        }
        try {
            keys = keys.stream().map(key -> _commonRedis.buildKeyWithPrefix(key)).collect(Collectors.toList());
            redisUnified.delete(keys.toArray(new String[keys.size()]));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public String getAndSet(String key, String item) throws Exception {
        try {
            return redisUnified.getAndSet(_commonRedis.buildKeyWithPrefix(key), item);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public Set<String> getKeysByPrefix(String prefix) throws Exception {
        try {
            Set<String> keysWithPrefix = redisUnified.getKeysByPrefix(_commonRedis.buildKeyWithPrefix(prefix));
            keysWithPrefix = keysWithPrefix.stream().map(key -> key.replace(_prefix + ".", "")).collect(Collectors.toSet());
            return keysWithPrefix;
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public long increment(String key, long value) throws Exception {
        try {
            return redisUnified.increment(_commonRedis.buildKeyWithPrefix(key), value);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public long hIncrement(String key, String field, long value) throws RedisException {
        try {
            return redisUnified.hIncrement(_commonRedis.buildKeyWithPrefix(key), field, value);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public long decrement(String key, long value) throws Exception {
        try {
            return redisUnified.decrement(_commonRedis.buildKeyWithPrefix(key), value);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public long pushItems(String key, List<String> items) throws Exception {
        try  {
            long addedItems = redisUnified.pushItems(_commonRedis.buildKeyWithPrefix(key), items.toArray(new String[items.size()]));
            if(EVENTS_KEY.equals(key) || IMPRESSIONS_KEY.equals(key)) {
                if(addedItems == items.size()) {
                    redisUnified.expire(key, IMPRESSIONS_OR_EVENTS_DEFAULT_TTL);
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
            return redisUnified.popItems(keyWithPrefix, count);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    // Return length of redis set.
    @Override
    public long getItemsCount(String key) throws Exception {
        try {
            return redisUnified.getItemsCount(_commonRedis.buildKeyWithPrefix(key));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public boolean itemContains(String key, String item) throws Exception {
        try {
            return redisUnified.itemContains(_commonRedis.buildKeyWithPrefix(key), item);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void addItems(String key, List<String> items) throws Exception {
        try {
            redisUnified.addItems(_commonRedis.buildKeyWithPrefix(key), items.toArray(new String[items.size()]));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void removeItems(String key, List<String> items) throws Exception {
        try {
            redisUnified.removeItems(_commonRedis.buildKeyWithPrefix(key), items.toArray(new String[items.size()]));
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
            return redisUnified.getItems(keys.toArray(new String[keys.size()]));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public boolean connect() throws Exception {
        try {
            return redisUnified.connect();
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public boolean disconnect() throws Exception {
        try {
            return redisUnified.disconnect();
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public Pipeline pipeline() throws Exception {
        try {
            // return new RedisPipeline(this.redisUnified, this._prefix);
            return null;
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }
}