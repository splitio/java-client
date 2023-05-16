package io.split.client;

import io.split.client.api.SplitView;

import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * An interface to manage an instance of Split SDK.
 */
public interface SplitManager {
    /**
     * Retrieves the feature flags that are currently registered with the
     * SDK.
     *
     * @return a List of SplitView or empty
     */
    List<SplitView> splits();

    /**
     * Returns the feature flag registered with the SDK of this name.
     *
     * @return SplitView or null
     */
    SplitView split(String featureFlagName);

    /**
     * Returns the names of feature flags registered with the SDK.
     *
     * @return a List of String (Feature Flag Names) or empty
     */
    List<String> splitNames();

    /**
     * The SDK kicks off background threads to download data necessary
     * for using the SDK. You can choose to block until the SDK has
     * downloaded feature flag definitions so that you will not get
     * the 'control' treatment.
     * <p>
     *
     * If the download is not successful in the time period set on
     * {@link SplitClientConfig.Builder#setBlockUntilReadyTimeout}, a TimeoutException will be thrown.
     * <p>
     */
    void blockUntilReady() throws TimeoutException, InterruptedException;
}