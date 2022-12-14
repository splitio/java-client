package io.split.engine.common;

import io.split.engine.experiments.FetchResult;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.SegmentSynchronizationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;


public class LocalhostSynchronizer implements Synchronizer{

    private static final Logger _log = LoggerFactory.getLogger(LocalhostSynchronizer.class);
    private final SplitSynchronizationTask _splitSynchronizationTask;
    private final SplitFetcher _splitFetcher;
    private final SegmentSynchronizationTask _segmentSynchronizationTaskImp;

    public LocalhostSynchronizer(SplitTasks splitTasks,
                                 SplitFetcher splitFetcher){
        _splitSynchronizationTask = checkNotNull(splitTasks.getSplitSynchronizationTask());
        _splitFetcher = checkNotNull(splitFetcher);
        _segmentSynchronizationTaskImp = splitTasks.getSegmentSynchronizationTask();
    }

    @Override
    public boolean syncAll() {
        FetchResult fetchResult = _splitFetcher.forceRefresh(new FetchOptions.Builder().cacheControlHeaders(true).build());
        return fetchResult.isSuccess();
    }

    @Override
    public void startPeriodicFetching() {
        _log.debug("Starting Periodic Fetching ...");
        _splitSynchronizationTask.start();
    }

    @Override
    public void stopPeriodicFetching() {
        _log.debug("Stop Periodic Fetching ...");
        _splitSynchronizationTask.stop();
    }

    @Override
    public void refreshSplits(Long targetChangeNumber) {
        FetchResult fetchResult = _splitFetcher.forceRefresh(new FetchOptions.Builder().cacheControlHeaders(true).build());
        if (fetchResult.isSuccess()){
            _log.debug("Refresh completed");
        } else {
            _log.debug("No changes fetched");
        }
    }

    @Override
    public void localKillSplit(String splitName, String defaultTreatment, long newChangeNumber) {
        //No-Op
    }

    @Override
    public void refreshSegment(String segmentName, long targetChangeNumber) {
        //Todo implement this method when SegmentChange is implemented for localhost.
    }

    @Override
    public void startPeriodicDataRecording() {
        //No-Op
    }

    @Override
    public void stopPeriodicDataRecording(long splitCount, long segmentCount, long segmentKeyCount) {
        //No-Op
    }
}
