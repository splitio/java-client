package io.split.client;

import io.split.client.api.Key;
import io.split.grammar.Treatments;

import java.util.Map;

/**
 * A SplitClient that ensures that all features are turned off for all users.
 * Useful for testing
 *
 * @author adil
 */
public class AlwaysReturnControlSplitClient implements SplitClient {

    @Override
    public String getTreatment(String key, String feature) {
        return Treatments.CONTROL;
    }

    @Override
    public String getTreatment(String key, String feature, Map<String, Object> attributes) {
        return getTreatment(key, feature);
    }

    @Override
    public String getTreatment(Key key, String feature, Map<String, Object> attributes) {
        return Treatments.CONTROL;
    }
}
