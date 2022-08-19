package redis;

import pluggable.CustomStorageWrapper;
import pluggable.HasPipelineSupport;
import pluggable.Pipeline;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
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

    private final JedisPool jedisPool;
    private final String _prefix;
    private final CommonRedis _commonRedis;

    public RedisImp(JedisPool jedisPool, String prefix) {
        this.jedisPool = jedisPool;
        this._prefix = prefix;
        _commonRedis = CommonRedis.create(prefix);
    }

    @Override
    public String get(String key) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
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
        try (Jedis jedis = this.jedisPool.getResource()) {
            keys = keys.stream().map(key -> _commonRedis.buildKeyWithPrefix(key)).collect(Collectors.toList());

            return jedis.mget(keys.toArray(new String[keys.size()]));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void set(String key, String item) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            if(key.contains(TELEMETRY_INIT)) {
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
    public void hSet(String key, String field, String json) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.hset(key, field, json);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void delete(List<String> keys) throws Exception {
        if(keys == null || keys.isEmpty()){
            return ;
        }
        try (Jedis jedis = this.jedisPool.getResource()) {
            keys = keys.stream().map(key -> _commonRedis.buildKeyWithPrefix(key)).collect(Collectors.toList());

            jedis.del(keys.toArray(new String[keys.size()]));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public String getAndSet(String key, String item) throws Exception {
        //Todo if this method isn't used we should deprecated
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.getSet(_commonRedis.buildKeyWithPrefix(key), item);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public Set<String> getKeysByPrefix(String prefix) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            Set<String> keysWithPrefix = jedis.keys(_commonRedis.buildKeyWithPrefix(prefix));
            keysWithPrefix = keysWithPrefix.stream().map(key -> key.replaceAll(_prefix + ".", "")).collect(Collectors.toSet());
            return keysWithPrefix;
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public long increment(String key, long value) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.incrBy(_commonRedis.buildKeyWithPrefix(key), value);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public long hIncrement(String key, String field, long value) throws RedisException {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.hincrBy(_commonRedis.buildKeyWithPrefix(key), field, value);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public long decrement(String key, long value) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.decrBy(_commonRedis.buildKeyWithPrefix(key), value);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public long pushItems(String key, List<String> items) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            long addedItems = jedis.rpush(_commonRedis.buildKeyWithPrefix(key), items.toArray(new String[items.size()]));
            if(EVENTS_KEY.equals(key) || IMPRESSIONS_KEY.equals(key)) {
                if(addedItems == items.size()) {
                    jedis.pexpire(key, IMPRESSIONS_OR_EVENTS_DEFAULT_TTL);
                }
            }
            return addedItems;
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public List<String> popItems(String key, long count) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            String keyWithPrefix =  _commonRedis.buildKeyWithPrefix(key);
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
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.scard(_commonRedis.buildKeyWithPrefix(key));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public boolean itemContains(String key, String item) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.sismember(_commonRedis.buildKeyWithPrefix(key), item);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void addItems(String key, List<String> items) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.sadd(_commonRedis.buildKeyWithPrefix(key), items.toArray(new String[items.size()]));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void removeItems(String key, List<String> items) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
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
        try (Jedis jedis = this.jedisPool.getResource()) {
            keys = keys.stream().map(key -> _commonRedis.buildKeyWithPrefix(key)).collect(Collectors.toList());

            return jedis.mget(keys.toArray(new String[keys.size()]));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public boolean connect() throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return "PONG".equalsIgnoreCase(jedis.ping());
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public boolean disconnect() throws Exception {
        try {
            jedisPool.close();

            return true;
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public Pipeline pipeline() throws Exception {
        try {
            return new RedisPipeline(this.jedisPool, this._prefix);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }
}
