package io.split.client.jmx;

/**
 * JMX Interface.
 * <p/>
 * Created by patricioe on 1/18/16.
 */
public interface SplitJmxMonitorMBean {

    /**
     * @returns TRUE if the sync features worked successfully.
     */
    boolean forceSyncFeatures();

    /**
     * @returns TRUE if the sync segments worked successfully.
     */
    boolean forceSyncSegment(String segmentName);

    /**
     * @param key         account of user key identifier
     * @param featureName the name of the feature
     * @return the evaluation of this feature for the identifier.
     */
    String getTreatment(String key, String featureName);

    /**
     * @param featureName
     * @return the feature definition
     */
    String fetchDefinition(String featureName);

    /**
     * @param key
     * @return TRUE if the key is in the segment
     */
    boolean isKeyInSegment(String key, String segmentName);

}
