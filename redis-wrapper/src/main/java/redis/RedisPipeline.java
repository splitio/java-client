package redis;

import pluggable.PipelineWrapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import java.util.List;

public class RedisPipeline implements PipelineWrapper {
    private Pipeline pipelined;
    private final String prefix;


    public RedisPipeline(JedisPool jedisPool, String prefix) {
        this.prefix = prefix;
        try (Jedis jedis = jedisPool.getResource()) {
            this.pipelined = jedis.pipelined();
        } catch (Exception ex) {
            System.out.println("err " + ex.getMessage());
        }
    }

        /* package private */ String buildKeyWithPrefix(String key) {
            if (!key.startsWith(this.prefix)) {
                key = String.format("%s.%s", prefix, key);
            }
    
            return key;
        }

    @Override
    public void increment(String key, long value) throws Exception {
        this.pipelined.incrBy(buildKeyWithPrefix(key), value);
    }

    @Override
    public List<Object> exec() throws Exception {
        return this.pipelined.syncAndReturnAll();
    }

}
