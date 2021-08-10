package io.split.storages.pluggable;

import java.util.List;

public interface CustomStorageWrapper {
    // key-value operations
    String get(String key);
    String getMany(List<String> keys);
    void set(String key, String item);
    void delete(List<String> keys);
    String getByPrefix(String prefix);
    String getAndSet(String key, String item);
    String getKeysByPrefix(String prefix);

    // integer operations
    void increment(String key, long value);
    void decrement(String key, long value);

    // queue operations
    void pushItems(String key, String items);
    String popItems(String key, long count);
    long getItemsCount(String key);

    // set operations
    boolean itemContains(String key, String item);
    void addItems(String key, String items);
    void removeItems(String key, String items);
    String getItems(List<String> keys);
}
