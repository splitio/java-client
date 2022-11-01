package redis;

import pluggable.Pipeline;

import java.util.List;
import java.util.Set;

public interface RedisUnified {

    String get(String key) throws Exception;

    List<String> getMany(String[] keys) throws Exception;

    void set(String key, String item) throws Exception;

    void hSet(String key, String field, String item) throws Exception;

    void delete(String[] keys) throws Exception;

    String getAndSet(String key, String item) throws Exception;

    Set<String> getKeysByPrefix(String prefix) throws Exception;

    long increment(String key, long value) throws Exception;

    long hIncrement(String key, String field, long value) throws RedisException;

    long decrement(String key, long value) throws Exception;

    long pushItems(String key, String[] items) throws Exception;

    List<String> popItems(String key, long count) throws Exception;

    long getItemsCount(String key) throws Exception;

    boolean itemContains(String key, String item) throws Exception;

    void addItems(String key, String[] items) throws Exception;

    void removeItems(String key, String[] items) throws Exception;

    List<String> getItems(String[] keys) throws Exception;

    boolean connect() throws Exception;

    boolean disconnect() throws Exception;

    void expire(String key, long ttl) throws Exception;

    Pipeline pipeline() throws Exception;
}
