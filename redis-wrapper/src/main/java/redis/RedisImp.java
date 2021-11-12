package redis;

import pluggable.CustomStorageWrapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class RedisImp implements CustomStorageWrapper {
    private static final String TELEMETRY_INIT = "SPLITIO.telemetry.init" ;

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
            throw new Exception(ex);
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
            throw new Exception(ex);
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
            throw new Exception(ex);
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
            throw new Exception(ex);
        }
    }

    @Override
    public String getAndSet(String key, String item) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.getSet(buildKeyWithPrefix(key), item);
        } catch (Exception ex) {
            throw new Exception(ex);
        }
    }

    @Override
    public Set<String> getKeysByPrefix(String prefix) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.keys(buildKeyWithPrefix(prefix));
        } catch (Exception ex) {
            throw new Exception(ex);
        }
    }

    @Override
    public long increment(String key, long value) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.incrBy(buildKeyWithPrefix(key), value);
        } catch (Exception ex) {
            throw new Exception(ex);
        }
    }

    @Override
    public long decrement(String key, long value) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.decrBy(buildKeyWithPrefix(key), value);
        } catch (Exception ex) {
            throw new Exception(ex);
        }
    }

    @Override
    public long pushItems(String key, List<String> items) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.rpush(buildKeyWithPrefix(key), items.toArray(new String[items.size()]));
        } catch (Exception ex) {
            throw new Exception(ex);
        }
    }

    @Override
    public List<String> popItems(String key, long count) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.rpop(buildKeyWithPrefix(key), (int)count);
        } catch (Exception ex) {
            throw new Exception(ex);
        }
    }

    // Return length of redis set.
    @Override
    public long getItemsCount(String key) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.scard(buildKeyWithPrefix(key));
        } catch (Exception ex) {
            throw new Exception(ex);
        }
    }

    @Override
    public boolean itemContains(String key, String item) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return jedis.sismember(buildKeyWithPrefix(key), item);
        } catch (Exception ex) {
            throw new Exception(ex);
        }
    }

    @Override
    public void addItems(String key, List<String> items) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.sadd(buildKeyWithPrefix(key), items.toArray(new String[items.size()]));
        } catch (Exception ex) {
            throw new Exception(ex);
        }
    }

    @Override
    public void removeItems(String key, List<String> items) throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.srem(buildKeyWithPrefix(key), items.toArray(new String[items.size()]));
        } catch (Exception ex) {
            throw new Exception(ex);
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
            throw new Exception(ex);
        }
    }

    @Override
    public boolean connect() throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            return "PONG".equalsIgnoreCase(jedis.ping());
        } catch (Exception ex) {
            throw new Exception(ex);
        }
    }

    @Override
    public boolean close() throws Exception {
        try {
            jedisPool.close();

            return true;
        } catch (Exception ex) {
            throw new Exception(ex);
        }
    }

    private String buildKeyWithPrefix(String key) {
        if (!key.startsWith(this.prefix)) {
            key = String.format("%s.%s", prefix, key);
        }

        return key;
    }
}

