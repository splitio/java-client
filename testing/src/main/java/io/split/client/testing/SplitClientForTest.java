package io.split.client.testing;

import io.split.client.SplitClient;
import io.split.client.api.Key;
import io.split.grammar.Treatments;

import java.util.HashMap;
import java.util.Map;
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

    public String getTreatment(String key, String split) {
        return _tests.containsKey(split)
                ? _tests.get(split)
                : "control";
    }

    public String getTreatment(String key, String split, Map<String, Object> attributes) {
        return this.getTreatment(key, split);
    }

    public String getTreatment(Key key, String split, Map<String, Object> attributes) {
        return _tests.containsKey(split)
                ? _tests.get(split)
                : Treatments.CONTROL;
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
    public void blockUntilReady(int waitInMilliseconds) throws TimeoutException, InterruptedException {

    }
}
