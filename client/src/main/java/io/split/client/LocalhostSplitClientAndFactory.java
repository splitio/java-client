package io.split.client;

import io.split.client.api.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of SplitClient that considers all partitions
 * passed in the constructor to be 100% on for all users, and
 * any other split to be 100% off for all users. This implementation
 * is useful for using Split in localhost environment.
 *
 * @author adil
 */
public final class LocalhostSplitClientAndFactory implements SplitClient {
    private static final Logger _log = LoggerFactory.getLogger(LocalhostSplitClientAndFactory.class);

    private LocalhostSplitFactory _factory;
    private LocalhostSplitClient _splitClient;

    public LocalhostSplitClientAndFactory(LocalhostSplitFactory container, LocalhostSplitClient client) {
        _factory = container;
        _splitClient = client;

        checkNotNull(_factory);
        checkNotNull(_splitClient);
    }

    @Override
    public String getTreatment(String key, String split) {
        return _splitClient.getTreatment(key, split);
    }

    @Override
    public String getTreatment(String key, String split, Map<String, Object> attributes) {
        return _splitClient.getTreatment(key, split, attributes);
    }

    @Override
    public String getTreatment(Key key, String split, Map<String, Object> attributes) {
        return _splitClient.getTreatment(key.matchingKey(), split, attributes);
    }

    public void updateFeatureToTreatmentMap(Map<SplitAndKey, String> map) {
        if (map  == null) {
            _log.warn("A null map was passed as an update. Ignoring this update.");
            return;
        }
        _splitClient.updateFeatureToTreatmentMap(map);
    }

    @Override
    public void destroy() {
        _factory.destroy();
        _splitClient.destroy();
    }

    @Override
    public boolean track(String key, String trafficType, String eventType) {
        return _splitClient.track(key, trafficType, eventType);
    }

    @Override
    public boolean track(String key, String trafficType, String eventType, double value) {
        return _splitClient.track(key, trafficType, eventType, value);
    }

    @Override
    public void blockUntilReady(int waitInMilliseconds) throws TimeoutException, InterruptedException {
        _splitClient.blockUntilReady(waitInMilliseconds);
    }

}
