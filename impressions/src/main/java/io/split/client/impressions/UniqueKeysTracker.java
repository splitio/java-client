package io.split.client.impressions;

public interface UniqueKeysTracker {

    boolean track (String featureName, String key);
    void start();
    void stop();
}
