package redis;

import org.junit.Assert;
import org.junit.Test;
import pluggable.Result;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class RedisPipelineTest {

    @Test
    public void testHincrement() throws Exception {
        RedisPipeline redisPipeline =  new RedisPipeline(new JedisPool(), "test-prefix:.");
        redisPipeline.hIncrement("test", "key1", 1L);
        redisPipeline.hIncrement("test", "key1", 1L);

        List<Result> results = redisPipeline.exec();
        Assert.assertEquals(Optional.of(1L), results.get(0).asLong());
        Assert.assertEquals(Optional.of(2L), results.get(1).asLong());
        List<String> keys = new ArrayList<>();

        keys.add("test");
        redisPipeline.delete(keys);
    }

    @Test
    public void testGetMembers() throws Exception {
        JedisPool jedisPool = new JedisPool();
        RedisPipeline redisPipeline =  new RedisPipeline(jedisPool, "");
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.sadd("set1", "flag1", "flag2", "flag3");
            jedis.sadd("set2", "flag6", "flag5");

            redisPipeline.getMembers("set1");
            redisPipeline.getMembers("set2");

            List<Result> results = redisPipeline.exec();

            Assert.assertEquals(3, results.get(0).asHash().get().size());
            Assert.assertEquals(2, results.get(1).asHash().get().size());
        } catch (Exception ex) {
            throw new RedisException(ex.getMessage());
        } finally {
            redisPipeline.delete(new ArrayList<>(Arrays.asList("set1", "set2")));
        }
    }

}