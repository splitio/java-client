package io.split.client.jmx;

import io.split.storages.SegmentCache;
import io.split.storages.SplitCache;
import io.split.client.SplitClient;
import io.split.engine.common.FetchOptions;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.segments.SegmentFetcher;
import io.split.engine.segments.SegmentSynchronizationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by patricioe on 1/18/16.
 */
public class SplitJmxMonitor implements SplitJmxMonitorMBean {

    private static final Logger _log = LoggerFactory.getLogger(SplitJmxMonitor.class);

    private final SplitClient _client;
    private final SplitFetcher _featureFetcher;
    private final SplitCache _splitCache;
    private final SegmentSynchronizationTask _segmentSynchronizationTask;
    private SegmentCache _segmentCache;

    public SplitJmxMonitor(SplitClient splitClient, SplitFetcher featureFetcher, SplitCache splitCache, SegmentSynchronizationTask segmentSynchronizationTask, SegmentCache segmentCache) {
        _client = checkNotNull(splitClient);
        _featureFetcher = checkNotNull(featureFetcher);
        _splitCache = checkNotNull(splitCache);
        _segmentSynchronizationTask = checkNotNull(segmentSynchronizationTask);
        _segmentCache = checkNotNull(segmentCache);
    }

    @Override
    public boolean forceSyncFeatures() {
        _featureFetcher.forceRefresh(new FetchOptions.Builder().cacheControlHeaders(true).build());
        _log.info("Features successfully refreshed via JMX");
        return true;
    }

    @Override
    public boolean forceSyncSegment(String segmentName) {
        SegmentFetcher fetcher = _segmentSynchronizationTask.getFetcher(segmentName);
        try{
            fetcher.fetch(new FetchOptions.Builder().build());
        }
        //We are sure this will never happen because getFetcher firts initiate the segment. This try/catch is for safe only.
        catch (NullPointerException np){
            throw new NullPointerException();
        }

        _log.info("Segment " + segmentName + " successfully refreshed via JMX");
        return true;
    }

    @Override
    public String getTreatment(String key, String featureName) {
        return _client.getTreatment(key, featureName);
    }

    @Override
    public String fetchDefinition(String featureName) {
        return _splitCache.get(featureName).toString();
    }

    @Override
    public boolean isKeyInSegment(String key, String segmentName) {
        return _segmentCache.isInSegment(segmentName, key);
    }
}
