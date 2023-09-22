package pluggable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface CustomStorageWrapper {
    // key-value operations
    String get(String key) throws Exception;
    List<String> getMany(List<String> keys) throws Exception;
    void set(String key, String item) throws Exception;
    void hSet(String key, String field, String item) throws Exception;
    void delete(List<String> keys) throws Exception;
    String getAndSet(String key, String item) throws Exception;
    Set<String> getKeysByPrefix(String prefix) throws Exception;

    // integer operations
    long increment(String key, long value) throws Exception;
    long decrement(String key, long value) throws Exception;
    long hIncrement(String key, String field, long value) throws Exception;

    // queue operations
    long pushItems(String key, List<String> items) throws Exception;
    List<String> popItems(String key, long count) throws Exception;
    long getItemsCount(String key) throws Exception;

    // set operations
    boolean itemContains(String key, String item) throws Exception;
    void addItems(String key, List<String> items) throws Exception;
    void removeItems(String key, List<String> items) throws Exception;
    List<String> getItems(List<String> keys) throws Exception;
    HashSet<String> getMembers(String key) throws Exception;
    boolean connect() throws Exception;
    boolean disconnect() throws Exception;
    Pipeline pipeline() throws Exception;
}