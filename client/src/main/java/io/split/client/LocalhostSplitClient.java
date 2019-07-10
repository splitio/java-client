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
    private static SplitResult SPLIT_RESULT_CONTROL = new SplitResult(Treatments.CONTROL, null);

    private Map<SplitAndKey, LocalhostSplit> _map;

    public LocalhostSplitClient(Map<SplitAndKey, LocalhostSplit> map) {
        checkNotNull(map, "map must not be null");
        _map = map;
    }

    @Override
    public String getTreatment(String key, String split) {
        return getTreatmentAndConfigInternal(key, split).treatment();
    }

    @Override
    public String getTreatment(String key, String split, Map<String, Object> attributes) {
        return getTreatmentAndConfigInternal(key, split).treatment();
    }

    @Override
    public String getTreatment(Key key, String split, Map<String, Object> attributes) {
        return getTreatmentAndConfigInternal(key.matchingKey(), split, attributes).treatment();
    }

    @Override
    public SplitResult getTreatmentWithConfig(String key, String split) {
        return getTreatmentAndConfigInternal(key, split);
    }

    @Override
    public SplitResult getTreatmentWithConfig(String key, String split, Map<String, Object> attributes) {
        return getTreatmentAndConfigInternal(key, split, attributes);
    }

    @Override
    public SplitResult getTreatmentWithConfig(Key key, String split, Map<String, Object> attributes) {
        return getTreatmentAndConfigInternal(key.matchingKey(), split, attributes);
    }

    private SplitResult getTreatmentAndConfigInternal(String key, String split) {
        return getTreatmentAndConfigInternal(key, split, null);
    }

    private SplitResult getTreatmentAndConfigInternal(String key, String split, Map<String, Object> attributes) {
        if (key == null || split == null) {
            return SPLIT_RESULT_CONTROL;
        }

        SplitAndKey override = SplitAndKey.of(split, key);
        if (_map.containsKey(override)) {
            return toSplitResult(_map.get(override));
        }

        SplitAndKey splitDefaultTreatment = SplitAndKey.of(split);

        LocalhostSplit localhostSplit = _map.get(splitDefaultTreatment);

        if (localhostSplit == null) {
            return SPLIT_RESULT_CONTROL;
        }

        return toSplitResult(localhostSplit);
    }

    private SplitResult toSplitResult(LocalhostSplit localhostSplit) {
        return new SplitResult(localhostSplit.treatment,localhostSplit.config);
    }

    public void updateFeatureToTreatmentMap(Map<SplitAndKey, LocalhostSplit> map) {
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
        // LocalhostSplitClient is always ready
    }

}
