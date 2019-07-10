package io.split.client;

import io.split.client.api.Key;
import io.split.client.api.SplitResult;
import io.split.grammar.Treatments;

import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * A SplitClient that ensures that all features are turned off for all users.
 * Useful for testing
 *
 * @author adil
 */
public class AlwaysReturnControlSplitClient implements SplitClient {

    private static final SplitResult RESULT_CONTROL = new SplitResult(Treatments.CONTROL, null);

    @Override
    public String getTreatment(String key, String split) {
        return Treatments.CONTROL;
    }

    @Override
    public String getTreatment(String key, String split, Map<String, Object> attributes) {
        return Treatments.CONTROL;
    }

    @Override
    public String getTreatment(Key key, String split, Map<String, Object> attributes) {
        return Treatments.CONTROL;
    }

    @Override
    public SplitResult getTreatmentWithConfig(String key, String split) {
        return RESULT_CONTROL;
    }

    @Override
    public SplitResult getTreatmentWithConfig(String key, String split, Map<String, Object> attributes) {
        return RESULT_CONTROL;
    }

    @Override
    public SplitResult getTreatmentWithConfig(Key key, String split, Map<String, Object> attributes) {
        return RESULT_CONTROL;
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
    public boolean track(String key, String trafficType, String eventType, long timestamp) {
        return false;
    }

    @Override
    public boolean track(String key, String trafficType, String eventType, double value, Map<String, Object> properties) {
        return false;
    }

    @Override
    public boolean track(String key, String trafficType, String eventType, double value, long timestamp) {
        return false;
    }

    @Override
    public boolean track(String key, String trafficType, String eventType, Map<String, Object> properties, long timestamp) {
        return false;
    }

    @Override
    public boolean track(String key, String trafficType, String eventType, double value, Map<String, Object> properties, long timestamp) {
        return false;
    }

    @Override
    public void blockUntilReady() throws TimeoutException, InterruptedException {
        //AlwaysReturnControl is always ready
    }

}
