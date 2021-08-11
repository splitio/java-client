package io.split.storages.pluggable;

import java.util.List;

public interface CustomStorageWrapper {
    // key-value operations
    String get(String key) throws Exception;
    String getMany(List<String> keys) throws Exception;
    void set(String key, String item) throws Exception;
    void delete(List<String> keys) throws Exception;
    String getByPrefix(String prefix) throws Exception;
    String getAndSet(String key, String item) throws Exception;
    String getKeysByPrefix(String prefix) throws Exception;

    // integer operations
    void increment(String key, long value) throws Exception;
    void decrement(String key, long value) throws Exception;

    // queue operations
    void pushItems(String key, String items) throws Exception;
    String popItems(String key, long count) throws Exception;
    long getItemsCount(String key);

    // set operations
    boolean itemContains(String key, String item) throws Exception;
    void addItems(String key, String items) throws Exception;
    void removeItems(String key, String items) throws Exception;
    String getItems(List<String> keys) throws Exception;
}
