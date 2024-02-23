package io.split.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.split.client.api.SplitView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

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

    private Map<SplitAndKey, LocalhostSplit> _splitAndKeyToTreatmentMap;
    private Map<String, Set<String>> _splitToTreatmentsMap;

    public static LocalhostSplitManager of(Map<SplitAndKey, LocalhostSplit> featureToTreatmentMap) {
        checkNotNull(featureToTreatmentMap, "featureToTreatmentMap must not be null");
        return new LocalhostSplitManager(featureToTreatmentMap, splitsToTreatments(featureToTreatmentMap));
    }

    private static Map<String, Set<String>> splitsToTreatments(Map<SplitAndKey, LocalhostSplit> splitAndKeyStringMap) {
        Map<String, Set<String>> splitsToTreatments = Maps.newHashMap();
        for (Map.Entry<SplitAndKey, LocalhostSplit> entry : splitAndKeyStringMap.entrySet()) {
            String split = entry.getKey().split();
            if (!splitsToTreatments.containsKey(split)) {
                splitsToTreatments.put(split, Sets.<String>newHashSet());
            }
            Set<String> treatments = splitsToTreatments.get(split);
            treatments.add(entry.getValue().treatment);
        }
        return splitsToTreatments;
    }

    private LocalhostSplitManager(Map<SplitAndKey, LocalhostSplit> featureToTreatmentMap,  Map<String, Set<String>> splitToTreatmentsMap) {
        checkNotNull(featureToTreatmentMap, "featureToTreatmentMap must not be null");
        _splitAndKeyToTreatmentMap = featureToTreatmentMap;
        _splitToTreatmentsMap = splitToTreatmentsMap;
    }

    @Override
    public List<SplitView> splits() {
        List<SplitView> result = new ArrayList<SplitView>();

        for (Map.Entry<String, Set<String>> entry : _splitToTreatmentsMap.entrySet()) {
            result.add(toSplitView(entry.getKey(), entry.getValue()));
        }

        return result;
    }

    @Override
    public List<String> splitNames() {
        Set<String> splits = Sets.newHashSet();
        for (Map.Entry<SplitAndKey, LocalhostSplit> entry : _splitAndKeyToTreatmentMap.entrySet()) {
            splits.add(entry.getKey().split());
        }
        return Lists.newArrayList(splits);
    }

    @Override
    public void blockUntilReady() throws TimeoutException, InterruptedException {
        // It is always ready
    }

    @Override
    public SplitView split(String featureFlagName) {
        if (!_splitToTreatmentsMap.containsKey(featureFlagName)) {
            return null;
        }

        return toSplitView(featureFlagName, _splitToTreatmentsMap.get(featureFlagName));
    }

    void updateFeatureToTreatmentMap(Map<SplitAndKey, LocalhostSplit> featureToTreatmentMap) {
        checkNotNull(featureToTreatmentMap, "featureToTreatmentMap must not be null");
        _splitAndKeyToTreatmentMap = featureToTreatmentMap;
        _splitToTreatmentsMap = splitsToTreatments(_splitAndKeyToTreatmentMap);
    }


    private SplitView toSplitView(String featureFlagName, Set<String> treatments) {
        SplitView view = new SplitView();
        view.name = featureFlagName;
        view.killed = false;
        view.trafficType = null;
        view.changeNumber = 0;
        view.treatments = new ArrayList<String>();
        if (treatments != null) {
            view.treatments.addAll(treatments);
        }

        return view;
    }


}
