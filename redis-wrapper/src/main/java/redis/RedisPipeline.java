package redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pluggable.Result;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import java.util.List;
import java.util.stream.Collectors;

public class RedisPipeline implements pluggable.Pipeline {
    private Pipeline _pipelined;
    private final String _prefix;
    private final JedisPool _jedisPool;

    private static final Logger _log = LoggerFactory.getLogger(RedisPipeline.class);

    public RedisPipeline(JedisPool jedisPool, String prefix) {
        _jedisPool = jedisPool;
        _prefix = prefix;
        try (Jedis jedis = _jedisPool.getResource()) {
            _pipelined = jedis.pipelined();
        } catch (Exception ex) {
            new RedisException(ex.getMessage());
        }
    }

    /* package private */ String buildKeyWithPrefix(String key) {
        if (!key.startsWith(_prefix)) {
            key = String.format("%s.%s", _prefix, key);
        }

        return key;
    }

    @Override
    public void hIncrement(String key, String field, long value) {
        _pipelined.hincrBy(buildKeyWithPrefix(key), field, value);
    }

    public void delete(List<String> keys){
        if(keys == null || keys.isEmpty()){
            return ;
        }
        try (Jedis jedis = _jedisPool.getResource()) {
            keys = keys.stream().map(key -> buildKeyWithPrefix(key)).collect(Collectors.toList());

            jedis.del(keys.toArray(new String[keys.size()]));
        } catch (Exception ex) {
            new RedisException(ex.getMessage());
        }
    }

    @Override
    public List<Result> exec() throws Exception {
        try{
            List<Object> executionResult = _pipelined.syncAndReturnAll();
            return executionResult.stream().map(i -> new Result(i)).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RedisException(e.getMessage());
        }
    }
}
