package io.split.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ApiKeyCounter {

    private static final Logger _log = LoggerFactory.getLogger(ApiKeyCounter.class);
    private static final Multiset<String> USED_API_KEYS = ConcurrentHashMultiset.create();

    private ApiKeyCounter() {}

    public static ApiKeyCounter getApiKeyCounterInstance() {
        return ApyKeyCounterHolder.INSTANCE;
    }

    //Inner class to provide instance of class
    private static class ApyKeyCounterHolder
    {
        private static final ApiKeyCounter INSTANCE = new ApiKeyCounter();
    }

    public void add(String sdkKey) {
        String message;
        if (USED_API_KEYS.contains(sdkKey)) {
            message = String.format("factory instantiation: You already have %s with this SDK Key. " +
                            "We recommend keeping only one instance of the factory at all times (Singleton pattern) and reusing " +
                            "it throughout your application.",
                    USED_API_KEYS.count(sdkKey) == 1 ? "1 factory" : String.format("%s factories", USED_API_KEYS.count(sdkKey)));
            _log.warn(message);
        } else if (!USED_API_KEYS.isEmpty()) {
             message = "factory instantiation: You already have an instance of the Split factory. " +
                    "Make sure you definitely want this additional instance. We recommend keeping only one instance of " +
                    "the factory at all times (Singleton pattern) and reusing it throughout your application.“";
            _log.warn(message);
        }
        USED_API_KEYS.add(sdkKey);
    }

    public void remove(String sdkKey) {
        USED_API_KEYS.remove(sdkKey);
    }

    /**
     * Just for test
     * @param sdkKey
     * @return
     */
    @VisibleForTesting
    boolean isApiKeyPresent(String sdkKey) {
        return USED_API_KEYS.contains(sdkKey);
    }

    /**
     * Just for test
     * @param sdkKey
     * @return
     */
    @VisibleForTesting
    int getCount(String sdkKey) {
        return USED_API_KEYS.count(sdkKey);
    }

    public Map<String, Long> getFactoryInstances() {
        Map<String, Long> factoryInstances = new HashMap<>();
        for (String factory :USED_API_KEYS) {
            factoryInstances.putIfAbsent(factory, new Long(getCount(factory)));
        }

        return factoryInstances;
    }

    public void clearApiKeys() {
        USED_API_KEYS.clear();
    }
}