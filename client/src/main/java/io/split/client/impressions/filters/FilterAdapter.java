package io.split.client.impressions.filters;

public interface FilterAdapter {

    boolean add(String featureName, String key);
    boolean contains(String featureName, String key);
    void clear();
}
