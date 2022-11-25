package io.split.engine.common;

public enum LocalhostSynchronizer implements Synchronizer{
    ;

    @Override
    public boolean syncAll() {
        return false;
    }

    @Override
    public void startPeriodicFetching() {

    }

    @Override
    public void stopPeriodicFetching() {

    }

    @Override
    public void refreshSplits(long targetChangeNumber) {

    }

    @Override
    public void localKillSplit(String splitName, String defaultTreatment, long newChangeNumber) {

    }

    @Override
    public void refreshSegment(String segmentName, long targetChangeNumber) {

    }

    @Override
    public void startPeriodicDataRecording() {

    }

    @Override
    public void stopPeriodicDataRecording(long splitCount, long segmentCount, long segmentKeyCount) {

    }
}
