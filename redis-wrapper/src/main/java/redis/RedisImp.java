package redis;

import pluggable.CustomStorageWrapper;
import pluggable.HasPipelineSupport;
import pluggable.Pipeline;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

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
    private final String prefix;

    public RedisImp(JedisPool jedisPool, String prefix) {
        this.jedisPool = jedisPool;
        this.prefix = prefix;
    }

    @Override
    public String get(String key) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.get(buildKeyWithPrefix(key));
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
            keys = keys.stream().map(key -> buildKeyWithPrefix(key)).collect(Collectors.toList());

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
                jedis.hset(buildKeyWithPrefix(splittedKey[0]), splittedKey[1], item);
                return;
            }
            jedis.set(buildKeyWithPrefix(key), item);
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
            keys = keys.stream().map(key -> buildKeyWithPrefix(key)).collect(Collectors.toList());

            jedis.del(keys.toArray(new String[keys.size()]));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public String getAndSet(String key, String item) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.getSet(buildKeyWithPrefix(key), item);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public Set<String> getKeysByPrefix(String prefix) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.keys(buildKeyWithPrefix(prefix));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public long increment(String key, long value) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.incrBy(buildKeyWithPrefix(key), value);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public long decrement(String key, long value) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.decrBy(buildKeyWithPrefix(key), value);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public long pushItems(String key, List<String> items) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            long addedItems = jedis.rpush(buildKeyWithPrefix(key), items.toArray(new String[items.size()]));
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
            String keyWithPrefix =  buildKeyWithPrefix(key);
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
            return jedis.scard(buildKeyWithPrefix(key));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public boolean itemContains(String key, String item) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.sismember(buildKeyWithPrefix(key), item);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void addItems(String key, List<String> items) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.sadd(buildKeyWithPrefix(key), items.toArray(new String[items.size()]));
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }

    @Override
    public void removeItems(String key, List<String> items) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.srem(buildKeyWithPrefix(key), items.toArray(new String[items.size()]));
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
            keys = keys.stream().map(key -> buildKeyWithPrefix(key)).collect(Collectors.toList());

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

    /* package private */ String buildKeyWithPrefix(String key) {
        if (!key.startsWith(this.prefix)) {
            key = String.format("%s.%s", prefix, key);
        }

        return key;
    }

    @Override
    public Pipeline pipeline() throws Exception {
        try {
            return new RedisPipeline(this.jedisPool, this.prefix);
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        }
    }
}
