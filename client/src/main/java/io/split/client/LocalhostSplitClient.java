package io.split.client;

import io.split.client.api.Key;
import io.split.client.api.SplitResult;
import io.split.grammar.Treatments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
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
public final class LocalhostSplitClient implements SplitClient {
    private static final Logger _log = LoggerFactory.getLogger(LocalhostSplitClient.class);

    private Map<SplitAndKey, String> _map;

    public LocalhostSplitClient(Map<SplitAndKey, String> map) {
        checkNotNull(map, "map must not be null");
        _map = map;
    }

    @Override
    public String getTreatment(String key, String split) {
        if (key == null || split == null) {
            return Treatments.CONTROL;
        }

        SplitAndKey override = SplitAndKey.of(split, key);
        if (_map.containsKey(override)) {
            return _map.get(override);
        }

        SplitAndKey splitDefaultTreatment = SplitAndKey.of(split);

        String treatment = _map.get(splitDefaultTreatment);

        if (treatment == null) {
            return Treatments.CONTROL;
        }

        return treatment;
    }

    @Override
    public String getTreatment(String key, String split, Map<String, Object> attributes) {
        return getTreatment(key, split);
    }

    @Override
    public String getTreatment(Key key, String split, Map<String, Object> attributes) {
        return getTreatment(key.matchingKey(), split, attributes);
    }

    @Override
    public SplitResult getTreatmentWithConfig(String key, String split) {
        return new SplitResult(getTreatment(key, split), null);
    }

    @Override
    public SplitResult getTreatmentWithConfig(String key, String split, Map<String, Object> attributes) {
        return new SplitResult(getTreatment(key, split), null);
    }

    @Override
    public SplitResult getTreatmentWithConfig(Key key, String split, Map<String, Object> attributes) {
        return new SplitResult(getTreatment(key.matchingKey(), split, attributes), null);
    }

    public void updateFeatureToTreatmentMap(Map<SplitAndKey, String> map) {
        if (map  == null) {
            _log.warn("A null map was passed as an update. Ignoring this update.");
            return;
        }
        _map = map;
    }

    @Override
    public void destroy() {
        _map.clear();
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
    public void blockUntilReady() throws TimeoutException, InterruptedException {
        // LocalhostSplitClient is always ready
    }

}
