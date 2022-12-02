package io.split.engine.common;

import io.split.engine.experiments.SplitFetcher;
import io.split.storages.SegmentCacheProducer;
import io.split.storages.SplitCacheProducer;


public class LocalhostSynchronizer extends SynchronizerImp{

    public LocalhostSynchronizer(SplitTasks splitTasks,
                                 SplitFetcher splitFetcher,
                                 SplitCacheProducer splitCacheProducer,
                                 SegmentCacheProducer segmentCacheProducer){
        super(splitTasks, splitFetcher, splitCacheProducer, segmentCacheProducer,0,0,0,false);
    }

    @Override
    public void refreshSplits(long targetChangeNumber) {
        //No-Op
    }

    @Override
    public void localKillSplit(String splitName, String defaultTreatment, long newChangeNumber) {
        //No-Op
    }

    @Override
    public void refreshSegment(String segmentName, long targetChangeNumber) {
        //No-Op
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
