package redis.common;

import org.junit.Assert;
import org.junit.Test;


public class CommonRedisTest {

    @Test
    public void testBuildKey(){
        CommonRedis commonRedisWithPrefix = CommonRedis.create("testing:");
        Assert.assertEquals("testing:.feature_flag1", commonRedisWithPrefix.buildKeyWithPrefix("feature_flag1"));

        CommonRedis commonRedisWithoutPrefix = CommonRedis.create("");
        Assert.assertEquals("feature_flag2", commonRedisWithoutPrefix.buildKeyWithPrefix("feature_flag2"));
    }
}