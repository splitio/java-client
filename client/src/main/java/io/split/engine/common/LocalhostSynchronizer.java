package io.split.engine.common;

import io.split.engine.experiments.FetchResult;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.SegmentFetcher;
import io.split.engine.segments.SegmentSynchronizationTask;
import io.split.engine.sse.dtos.SplitKillNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class LocalhostSynchronizer implements Synchronizer{

    private static final Logger _log = LoggerFactory.getLogger(LocalhostSynchronizer.class);
    private final SplitSynchronizationTask _splitSynchronizationTask;
    private final SplitFetcher _splitFetcher;
    private final SegmentSynchronizationTask _segmentSynchronizationTaskImp;
    private final boolean _refreshEnable;

    public LocalhostSynchronizer(SplitTasks splitTasks,
                                 SplitFetcher splitFetcher,
                                 boolean refreshEnable){
        _splitSynchronizationTask = checkNotNull(splitTasks.getSplitSynchronizationTask());
        _splitFetcher = checkNotNull(splitFetcher);
        _segmentSynchronizationTaskImp = splitTasks.getSegmentSynchronizationTask();
        _refreshEnable = refreshEnable;
    }

    @Override
    public boolean syncAll() {
        FetchResult fetchResult = _splitFetcher.forceRefresh(new FetchOptions.Builder().cacheControlHeaders(true).build());
        return fetchResult.isSuccess() && _segmentSynchronizationTaskImp.fetchAllSynchronous();
    }

    @Override
    public void startPeriodicFetching() {
        _log.debug("Starting Periodic Fetching ...");
        if(!_refreshEnable){
            _log.info("Refresh enable is false. The synchronization tasks are not going to start");
            return;
        }
        _splitSynchronizationTask.start();
        _segmentSynchronizationTaskImp.start();
    }

    @Override
    public void stopPeriodicFetching() {
        _log.debug("Stop Periodic Fetching ...");
        if(!_refreshEnable){
            return;
        }
        _splitSynchronizationTask.stop();
        _segmentSynchronizationTaskImp.stop();
    }

    @Override
    public void refreshSplits(Long targetChangeNumber) {
        FetchResult fetchResult = _splitFetcher.forceRefresh(new FetchOptions.Builder().cacheControlHeaders(true).build());
        if (fetchResult.isSuccess()){
            _log.debug("Refresh feature flags completed");
            fetchResult.getSegments().stream().forEach(segmentName -> refreshSegment(segmentName, null));
        } else {
            _log.debug("No changes fetched");
        }
    }

    @Override
    public void localKillSplit(SplitKillNotification splitKillNotification) {
        //No-Op
    }

    @Override
    public void refreshSegment(String segmentName, Long targetChangeNumber) {
        SegmentFetcher segmentFetcher = _segmentSynchronizationTaskImp.getFetcher(segmentName);
        segmentFetcher.fetch(new FetchOptions.Builder().cacheControlHeaders(true).build());
    }

    @Override
    public void startPeriodicDataRecording() {
        //No-Op
    }

    @Override
    public void stopPeriodicDataRecording() {
        //No-Op
    }
}
