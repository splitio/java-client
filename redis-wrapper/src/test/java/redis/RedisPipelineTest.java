package redis;

import org.junit.Assert;
import org.junit.Test;
import pluggable.Pipeline;
import pluggable.Result;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
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

}