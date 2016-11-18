package io.split.client;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of SplitClient that considers all partitions
 * passed in the constructor to be 100% on for all users, and
 * any other split to be 100% off for all users. This implementation
 * is useful for using Codigo in localhost environment.
 *
 * @author adil
 */
public final class LocalhostSplitFactory implements SplitFactory {

    private final ImmutableMap<String, String> _featureToTreatmentMap;

    public LocalhostSplitFactory(Map<String, String> featureToTreatmentMap) {
        checkNotNull(featureToTreatmentMap, "featureToTreatmentMap must not be null");
        _featureToTreatmentMap = ImmutableMap.copyOf(featureToTreatmentMap);
    }

    @Override
    public SplitClient client() {
        return new LocalhostSplitClient(_featureToTreatmentMap);
    }

    @Override
    public SplitManager manager() {
        return new LocalhostSplitManager(_featureToTreatmentMap);
    }
}
