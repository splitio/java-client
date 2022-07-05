package redis;

import pluggable.PipelineWrapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import java.util.List;

public class RedisPipeline implements PipelineWrapper {
    private Pipeline _pipelined;
    private final String _prefix;


    public RedisPipeline(JedisPool jedisPool, String prefix) {
        _prefix = prefix;
        try (Jedis jedis = jedisPool.getResource()) {
            _pipelined = jedis.pipelined();
        } catch (Exception ex) {
            System.out.println("err " + ex.getMessage());
        }
    }

    /* package private */ String buildKeyWithPrefix(String key) {
        if (!key.startsWith(_prefix)) {
            key = String.format("%s.%s", _prefix, key);
        }

        return key;
    }

    @Override
    public void increment(String key, long value) throws Exception {
        _pipelined.incrBy(buildKeyWithPrefix(key), value);
    }

    @Override
    public List<Object> exec() throws Exception {
        return _pipelined.syncAndReturnAll();
    }

}
