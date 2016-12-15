package io.split.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.split.client.api.SplitView;

import java.util.ArrayList;
import java.util.List;
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
public final class LocalhostSplitManager implements SplitManager {

    private ImmutableMap<String, String> _featureToTreatmentMap;

    public LocalhostSplitManager(Map<String, String> featureToTreatmentMap) {
        checkNotNull(featureToTreatmentMap, "featureToTreatmentMap must not be null");
        _featureToTreatmentMap = ImmutableMap.copyOf(featureToTreatmentMap);
    }

    @Override
    public List<SplitView> splits() {
        List<SplitView> result = new ArrayList<SplitView>();

        for (Map.Entry<String, String> entry : _featureToTreatmentMap.entrySet()) {
            result.add(toSplitView(entry.getKey(), entry.getValue()));
        }

        return result;
    }

    @Override
    public List<String> splitNames() {
        return Lists.newArrayList(_featureToTreatmentMap.keySet());
    }

    @Override
    public SplitView split(String featureName) {
        if (!_featureToTreatmentMap.containsKey(featureName)) {
            return null;
        }

        return toSplitView(featureName, _featureToTreatmentMap.get(featureName));
    }

    void updateFeatureToTreatmentMap(Map<String, String> featureToTreatmentMap) {
        checkNotNull(featureToTreatmentMap, "featureToTreatmentMap must not be null");
        _featureToTreatmentMap = ImmutableMap.copyOf(featureToTreatmentMap);
    }

    @VisibleForTesting
    ImmutableMap<String, String> featureToTreatmentMap() {
        return _featureToTreatmentMap;
    }

    private SplitView toSplitView(String featureName, String treatment) {
        SplitView view = new SplitView();
        view.name = featureName;
        view.killed = false;
        view.trafficType = null;
        view.changeNumber = 0;
        view.treatments = new ArrayList<String>();
        if (treatment != null) {
            view.treatments.add(treatment);
        }

        return view;
    }


}
