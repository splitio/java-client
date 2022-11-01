package redis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import pluggable.CustomStorageWrapper;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisInstance {

    private static final int TIMEOUT = 1000;

    public static Builder builder() {
        return new Builder();
    }

    private static CustomStorageWrapper getRedisInstance(String host, int port, int timeout, String user, String password, int database, String prefix, int maxTotal) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxTotal);
        JedisPool jedisPool = new JedisPool(poolConfig, host, port, timeout, password, database);
        RedisSingle redisSingle = new RedisSingle(jedisPool);
        return new RedisImp(redisSingle, prefix);
    }

    private static CustomStorageWrapper getRedisInstance(JedisPool jedisPool, String prefix) {
        return new RedisImp(new RedisSingle(jedisPool), prefix);
    }

    private static CustomStorageWrapper getRedisInstance(JedisCluster jedisCluster, String prefix) {
        return new RedisImp(new RedisCluster(jedisCluster), prefix);
    }

    public static final class Builder {
        private int _timeout = TIMEOUT;
        private String _host = "localhost";
        private int _port = 6379;
        private String _user = null;
        private String _password = null;
        private int _database = 0;
        private String _prefix = "";
        private JedisPool _jedisPool = null;
        private int _maxTotal = GenericObjectPoolConfig.DEFAULT_MAX_TOTAL;

        public Builder timeout(int timeout) {
            _timeout = timeout;
            return this;
        }

        public Builder host(String host) {
            _host = host;
            return this;
        }

        public Builder port(int port) {
            _port = port;
            return this;
        }

        public Builder user(String user) {
            _user = user;
            return this;
        }

        public Builder password(String password) {
            _password = password;
            return this;
        }

        public Builder database(int database) {
            _database = database;
            return this;
        }

        public Builder prefix(String prefix) {
            _prefix = prefix;
            return this;
        }

        public Builder jedisPool(JedisPool jedisPool) {
            _jedisPool = jedisPool;
            return this;
        }

        public Builder maxTotal(int _maxTotal) {
            _maxTotal = _maxTotal;
            return this;
        }

        public CustomStorageWrapper build() {
            if(_jedisPool != null) {
                return RedisInstance.getRedisInstance(_jedisPool, _prefix);
            }
            return RedisInstance.getRedisInstance(_host, _port, _timeout, _user, _password, _database, _prefix, _maxTotal);
        }
    }
}
