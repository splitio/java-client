package io.split.engine.common;

public enum LocalhostSynchronizer implements Synchronizer {
    ;

    @Override
    public boolean syncAll() {
        //Todo implement
        return false;
    }

    @Override
    public void startPeriodicFetching() {
        //todo implement
    }

    @Override
    public void stopPeriodicFetching() {
        // todo implement
    }

    @Override
    public void refreshSplits(long targetChangeNumber) {
        // todo implement
    }

    @Override
    public void localKillSplit(String splitName, String defaultTreatment, long newChangeNumber) {
        //No-Op
    }

    @Override
    public void refreshSegment(String segmentName, long targetChangeNumber) {
        // todo implement
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
