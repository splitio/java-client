package io.split.client.testing;

import io.split.client.SplitClient;
import io.split.client.api.Key;
import io.split.client.api.SplitResult;
import io.split.grammar.Treatments;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class SplitClientForTest implements SplitClient {
    private Map<String, String> _tests;

    public SplitClientForTest() {
        _tests = new HashMap<>();
    }

    public Map<String, String> tests() {
        return _tests;
    }

    public void clearTreatments() {
        _tests = new HashMap<>();
    }

    public void registerTreatments(Map<String, String> treatments) {
        _tests.putAll(treatments);
    }

    public void registerTreatment(String feature, String treatment) {
        _tests.put(feature, treatment);
    }

    public String getTreatment(String key, String featureFlagName) {
        return _tests.containsKey(featureFlagName)
                ? _tests.get(featureFlagName)
                : Treatments.CONTROL;
    }

    public String getTreatment(String key, String featureFlagName, Map<String, Object> attributes) {
        return _tests.containsKey(featureFlagName)
                ? _tests.get(featureFlagName)
                : Treatments.CONTROL;
    }

    public String getTreatment(Key key, String featureFlagName, Map<String, Object> attributes) {
        return _tests.containsKey(featureFlagName)
                ? _tests.get(featureFlagName)
                : Treatments.CONTROL;
    }

    @Override
    public SplitResult getTreatmentWithConfig(String key, String featureFlagName) {
        return new SplitResult(_tests.containsKey(featureFlagName)
                ? _tests.get(featureFlagName)
                : Treatments.CONTROL, null);
    }

    @Override
    public SplitResult getTreatmentWithConfig(String key, String featureFlagName, Map<String, Object> attributes) {
        return new SplitResult(_tests.containsKey(featureFlagName)
                ? _tests.get(featureFlagName)
                : Treatments.CONTROL, null);
    }

    @Override
    public SplitResult getTreatmentWithConfig(Key key, String featureFlagName, Map<String, Object> attributes) {
        return new SplitResult(_tests.containsKey(featureFlagName)
                ? _tests.get(featureFlagName)
                : Treatments.CONTROL, null);
    }

    @Override
    public Map<String, String> getTreatments(String key, List<String> featureFlagNames) {
        Map<String, String> treatments = new HashMap<>();
        for (String split : featureFlagNames) {
            treatments.put(split, _tests.containsKey(split) ? _tests.get(split) : Treatments.CONTROL);
        }
        return treatments;
    }

    @Override
    public Map<String, String> getTreatments(String key, List<String> featureFlagNames, Map<String, Object> attributes){
        Map<String, String> treatments = new HashMap<>();
        for (String split : featureFlagNames) {
            treatments.put(split, _tests.containsKey(split) ? _tests.get(split) : Treatments.CONTROL);
        }
        return treatments;
    }

    @Override
    public Map<String, String> getTreatments(Key key, List<String> featureFlagNames, Map<String, Object> attributes) {
        Map<String, String> treatments = new HashMap<>();
        for (String split : featureFlagNames) {
            treatments.put(split, _tests.containsKey(split) ? _tests.get(split) : Treatments.CONTROL);
        }
        return treatments;
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(String key, List<String> featureFlagNames) {
        Map<String, SplitResult> treatments = new HashMap<>();
        for (String split : featureFlagNames) {
            treatments.put(split, new SplitResult(_tests.containsKey(split)
            ? _tests.get(split)
            : Treatments.CONTROL, null));
        }
        return treatments;
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(String key, List<String> featureFlagNames, Map<String, Object> attributes) {
        Map<String, SplitResult> treatments = new HashMap<>();
        for (String split : featureFlagNames) {
            treatments.put(split, new SplitResult(_tests.containsKey(split)
            ? _tests.get(split)
            : Treatments.CONTROL, null));
        }
        return treatments;
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(Key key, List<String> featureFlagNames, Map<String, Object> attributes) {
        Map<String, SplitResult> treatments = new HashMap<>();
        for (String split : featureFlagNames) {
            treatments.put(split, new SplitResult(_tests.containsKey(split)
            ? _tests.get(split)
            : Treatments.CONTROL, null));
        }
        return treatments;
    }

    @Override
    public void destroy() {

    }

    @Override
    public boolean track(String key, String trafficType, String eventType) {
        return false;
    }

    @Override
    public boolean track(String key, String trafficType, String eventType, double value) {
        return false;
    }

    @Override
    public boolean track(String key, String trafficType, String eventType, Map<String, Object> properties) {
        return false;
    }

    @Override
    public boolean track(String key, String trafficType, String eventType, double value, Map<String, Object> properties) {
        return false;
    }

    @Override
    public void blockUntilReady() throws TimeoutException, InterruptedException {

    }
}
