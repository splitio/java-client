package io.split.client.impressions.filters;

public interface FilterAdapter {

    boolean add(String featureFlagName, String key);
    boolean contains(String featureFlagName, String key);
    void clear();
}
