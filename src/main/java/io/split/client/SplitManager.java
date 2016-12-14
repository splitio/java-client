package io.split.client;

import io.split.client.api.SplitView;

import java.util.List;

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
}
