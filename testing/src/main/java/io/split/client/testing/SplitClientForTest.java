package io.split.client.testing;

import io.split.client.SplitAndKey;
import io.split.client.SplitClient;
import io.split.client.api.Key;
import io.split.client.api.SplitResult;
import io.split.client.dtos.EvaluationOptions;
import io.split.grammar.Treatments;

import java.util.*;
import java.util.concurrent.TimeoutException;

public class SplitClientForTest implements SplitClient {
    private static final SplitResult CONTROL_RESULT = new SplitResult(Treatments.CONTROL, null);

    private Map<SplitAndKey, SplitResult> _tests;

    public SplitClientForTest() {
        _tests = new HashMap<>();
    }

    public Map<SplitAndKey, SplitResult> tests() {
        return _tests;
    }

    public void clearTreatments() {
        _tests.clear();
    }

    public void registerTreatments(Map<String, String> treatments) {
        for (Map.Entry<String, String> entry : treatments.entrySet()) {
            registerTreatment(entry.getKey(), entry.getValue());
        }
    }

    public void registerTreatment(String feature, String treatment) {
        registerTreatment(feature, null, treatment);
    }

    public void registerTreatment(String feature, String key, String treatment) {
        registerTreatment(feature, key, treatment, null);
    }

    public void registerTreatment(String feature, String key, String treatment, String config) {
        registerTreatment(SplitAndKey.of(feature, key), new SplitResult(treatment, config));
    }

    public void registerTreatment(SplitAndKey splitAndKey, SplitResult splitResult) {
        _tests.put(splitAndKey, splitResult);
    }

    public String getTreatment(String key, String featureFlagName) {
        return getTreatment(key, featureFlagName, Collections.emptyMap());
    }

    public String getTreatment(String key, String featureFlagName, Map<String, Object> attributes) {
        return getTreatmentWithConfig(key, featureFlagName, attributes).treatment();
    }

    public String getTreatment(Key key, String featureFlagName, Map<String, Object> attributes) {
        return getTreatment(key.matchingKey(), featureFlagName, attributes);
    }

    @Override
    public SplitResult getTreatmentWithConfig(String key, String featureFlagName) {
        return getTreatmentWithConfig(key, featureFlagName, Collections.emptyMap());
    }

    @Override
    public SplitResult getTreatmentWithConfig(String key, String featureFlagName, Map<String, Object> attributes) {
        if (_tests.containsKey(SplitAndKey.of(featureFlagName, key))) {
            return _tests.get(SplitAndKey.of(featureFlagName, key));
        }
        else {
            return _tests.getOrDefault(SplitAndKey.of(featureFlagName), CONTROL_RESULT);
        }
    }

    @Override
    public SplitResult getTreatmentWithConfig(Key key, String featureFlagName, Map<String, Object> attributes) {
        return getTreatmentWithConfig(key.matchingKey(), featureFlagName, attributes);
    }

    @Override
    public Map<String, String> getTreatments(String key, List<String> featureFlagNames) {
        return getTreatments(key, featureFlagNames, Collections.emptyMap());
    }

    @Override
    public Map<String, String> getTreatments(String key, List<String> featureFlagNames, Map<String, Object> attributes){
        Map<String, String> treatments = new HashMap<>();
        for (String split : featureFlagNames) {
            treatments.put(split, getTreatment(key, split, attributes));
        }
        return treatments;
    }

    @Override
    public Map<String, String> getTreatments(Key key, List<String> featureFlagNames, Map<String, Object> attributes) {
        return getTreatments(key.matchingKey(), featureFlagNames, attributes);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(String key, List<String> featureFlagNames) {
        return getTreatmentsWithConfig(key, featureFlagNames, Collections.emptyMap());
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(String key, List<String> featureFlagNames, Map<String, Object> attributes) {
        Map<String, SplitResult> treatments = new HashMap<>();
        for (String split : featureFlagNames) {
            treatments.put(split, getTreatmentWithConfig(key, split, attributes));
        }
        return treatments;
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(Key key, List<String> featureFlagNames, Map<String, Object> attributes) {
        return getTreatmentsWithConfig(key.matchingKey(), featureFlagNames, attributes);
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSet(String key, String flagSet) {
        return new HashMap<>();
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSet(String key, String flagSet, Map<String, Object> attributes) {
        return new HashMap<>();
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSet(Key key, String flagSet, Map<String, Object> attributes) {
        return new HashMap<>();
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSets(String key, List<String> flagSets) {
        return new HashMap<>();
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSets(String key, List<String> flagSets, Map<String, Object> attributes) {
        return new HashMap<>();
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSets(Key key, List<String> flagSets, Map<String, Object> attributes) {
        return new HashMap<>();
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(String key, String flagSet) {
        return new HashMap<>();
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(String key, String flagSet, Map<String, Object> attributes) {
        return new HashMap<>();
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(Key key, String flagSet, Map<String, Object> attributes) {
        return new HashMap<>();
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(String key, List<String> flagSets) {
        return new HashMap<>();
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(String key, List<String> flagSets, Map<String, Object> attributes) {
        return new HashMap<>();
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(Key key, List<String> flagSets, Map<String, Object> attributes) {
        return new HashMap<>();
    }

    @Override
    public String getTreatment(String key, String featureFlagName, EvaluationOptions evaluationOptions) {
        return getTreatment(key, featureFlagName);
    }

    @Override
    public String getTreatment(String key, String featureFlagName, Map<String, Object> attributes, EvaluationOptions evaluationOptions) {
        return getTreatment(key, featureFlagName, attributes);
    }

    @Override
    public String getTreatment(Key key, String featureFlagName, Map<String, Object> attributes, EvaluationOptions evaluationOptions) {
        return getTreatment(key, featureFlagName, attributes);
    }

    @Override
    public Map<String, String> getTreatments(String key, List<String> featureFlagNames, EvaluationOptions evaluationOptions) {
        return getTreatments(key, featureFlagNames);
    }

    @Override
    public Map<String, String> getTreatments(String key, List<String> featureFlagNames, Map<String, Object> attributes,
                                             EvaluationOptions evaluationOptions) {
        return getTreatments(key, featureFlagNames, attributes);
    }

    @Override
    public Map<String, String> getTreatments(Key key, List<String> featureFlagNames, Map<String, Object> attributes,
                                             EvaluationOptions evaluationOptions) {
        return getTreatments(key, featureFlagNames, attributes);
    }

    @Override
    public SplitResult getTreatmentWithConfig(String key, String featureFlagName, EvaluationOptions evaluationOptions) {
        return getTreatmentWithConfig(key, featureFlagName);
    }

    @Override
    public SplitResult getTreatmentWithConfig(Key key, String featureFlagName, Map<String, Object> attributes,
                                              EvaluationOptions evaluationOptions) {
        return getTreatmentWithConfig(key, featureFlagName, attributes);
    }

    @Override
    public SplitResult getTreatmentWithConfig(String key, String featureFlagName, Map<String, Object> attributes,
                                              EvaluationOptions evaluationOptions) {
        return getTreatmentWithConfig(key, featureFlagName, attributes);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(String key, List<String> featureFlagNames, Map<String, Object> attributes,
                                                            EvaluationOptions evaluationOptions) {
        return getTreatmentsWithConfig(key, featureFlagNames, attributes);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(String key, List<String> featureFlagNames, EvaluationOptions evaluationOptions) {
        return getTreatmentsWithConfig(key, featureFlagNames);
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSet(String key, String flagSet, Map<String, Object> attributes,
                                                      EvaluationOptions evaluationOptions) {
        return new HashMap<>();
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSets(String key, List<String> flagSets, EvaluationOptions evaluationOptions) {
        return new HashMap<>();
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSets(String key, List<String> flagSets, Map<String, Object> attributes,
                                                       EvaluationOptions evaluationOptions) {
        return new HashMap<>();
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(String key, String flagSet, EvaluationOptions evaluationOptions) {
        return new HashMap<>();
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(String key, String flagSet, Map<String, Object> attributes,
                                                                     EvaluationOptions evaluationOptions) {
        return new HashMap<>();
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(String key, List<String> flagSets, EvaluationOptions evaluationOptions) {
        return new HashMap<>();
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(String key, List<String> flagSets, Map<String, Object> attributes,
                                                                      EvaluationOptions evaluationOptions) {
        return new HashMap<>();
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSet(String key, String flagSet, EvaluationOptions evaluationOptions) {
        return new HashMap<>();
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(Key key, List<String> featureFlagNames, Map<String, Object> attributes,
                                                            EvaluationOptions evaluationOptions) {
        return new HashMap<>();
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSet(Key key, String flagSet, Map<String, Object> attributes,
                                                      EvaluationOptions evaluationOptions) {
        return new HashMap<>();
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSets(Key key, List<String> flagSets, Map<String, Object> attributes,
                                                       EvaluationOptions evaluationOptions) {
        return new HashMap<>();
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(Key key, String flagSet, Map<String, Object> attributes,
                                                                     EvaluationOptions evaluationOptions) {
        return new HashMap<>();
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(Key key, List<String> flagSets, Map<String, Object> attributes,
                                                                      EvaluationOptions evaluationOptions) {
        return new HashMap<>();
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
