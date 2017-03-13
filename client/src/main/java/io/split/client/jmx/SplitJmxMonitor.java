package io.split.client.jmx;

import io.split.client.SplitClient;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.segments.SegmentFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by patricioe on 1/18/16.
 */
public class SplitJmxMonitor implements SplitJmxMonitorMBean {

    private static final Logger _log = LoggerFactory.getLogger(SplitJmxMonitor.class);

    private final SplitClient _client;
    private final SplitFetcher _featureFetcher;
    private final SegmentFetcher _segmentFetcher;

    public SplitJmxMonitor(SplitClient splitClient, SplitFetcher fetcher, SegmentFetcher segmentFetcher) {
        _client = splitClient;
        _featureFetcher = fetcher;
        _segmentFetcher = segmentFetcher;
    }

    @Override
    public boolean forceSyncFeatures() {
        _featureFetcher.forceRefresh();
        _log.info("Features successfully refreshed via JMX");
        return true;
    }

    @Override
    public boolean forceSyncSegment(String segmentName) {
        _segmentFetcher.segment(segmentName).forceRefresh();
        _log.info("Segment " + segmentName + " successfully refreshed via JMX");
        return true;
    }

    @Override
    public String getTreatment(String key, String featureName) {
        return _client.getTreatment(key, featureName);
    }

    @Override
    public String fetchDefinition(String featureName) {
        return _featureFetcher.fetch(featureName).toString();
    }

    @Override
    public boolean isKeyInSegment(String key, String segmentName) {
        return _segmentFetcher.segment(segmentName).contains(key);
    }
}
