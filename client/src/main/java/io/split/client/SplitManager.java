package io.split.client;

import io.split.client.api.SplitView;

import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * An interface to manage an instance of Split SDK.
 */
public interface SplitManager {
    /**
     * Retrieves the features (or Splits) that are currently registered with the
     * SDK.
     *
     * @return a List of SplitView or empty
     */
    List<SplitView> splits();

    /**
     * Returns the feature (or Split) registered with the SDK of this name.
     *
     * @return SplitView or null
     */
    SplitView split(String featureName);

    /**
     * Returns the names of features (or Splits) registered with the SDK.
     *
     * @return a List of String (Split Feature Names) or empty
     */
    List<String> splitNames();

    /**
     * The SDK kicks off background threads to download data necessary
     * for using the SDK. You can choose to block until the SDK has
     * downloaded split definitions so that you will not get
     * the 'control' treatment.
     */
    void blockUntilReady() throws TimeoutException, InterruptedException;
}
