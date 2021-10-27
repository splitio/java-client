package redis;

import pluggable.CustomStorageWrapper;

public class RedisInstance {
    public static CustomStorageWrapper getRedisInstance(String url, int port, String prefix) {
        return new RedisImp(url, port, prefix);
    }
}
