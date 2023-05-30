package io.split.engine.common;

import io.split.client.impressions.ImpressionsManager;
import io.split.client.impressions.UniqueKeysTracker;
import io.split.engine.sse.dtos.SplitKillNotification;
import io.split.telemetry.synchronizer.TelemetrySyncTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerSynchronizer implements Synchronizer{

    private static final Logger _log = LoggerFactory.getLogger(ConsumerSynchronizer.class);
    private  final UniqueKeysTracker _uniqueKeysTracker;
    private final ImpressionsManager _impressionManager;
    private final TelemetrySyncTask _telemetrySyncTask;

    public ConsumerSynchronizer(SplitTasks splitTasks){
        _uniqueKeysTracker = splitTasks.getUniqueKeysTracker();
        _impressionManager = splitTasks.getImpressionManager();
        _telemetrySyncTask = splitTasks.getTelemetrySyncTask();
    }
    @Override
    public boolean syncAll() {
        return false;
    }

    @Override
    public void startPeriodicFetching() {
        //No-Op
    }

    @Override
    public void stopPeriodicFetching() {
        //No-Op
    }

    @Override
    public void refreshSplits(Long targetChangeNumber) {
        //No-Op
    }

    @Override
    public void localKillSplit(SplitKillNotification splitKillNotification) {
        //No-Op
    }

    @Override
    public void refreshSegment(String segmentName, Long targetChangeNumber) {
        //No-Op
    }

    @Override
    public void startPeriodicDataRecording() {
        try {
            _impressionManager.start();
        } catch (Exception e) {
            _log.error("Error trying to init Impression Manager synchronizer task.", e);
        }
        if (_uniqueKeysTracker != null){
            try {
                _uniqueKeysTracker.start();
            } catch (Exception e) {
                _log.error("Error trying to init Unique Keys Tracker synchronizer task.", e);
            }
        }
        try {
            _telemetrySyncTask.startScheduledTask();
        } catch (Exception e) {
            _log.error("Error trying to Telemetry synchronizer task.", e);
        }
    }

    @Override
    public void stopPeriodicDataRecording() {
        _impressionManager.close();
        _log.info("Successful shutdown of impressions manager");
        if (_uniqueKeysTracker != null){
            _uniqueKeysTracker.stop();
            _log.info("Successful stop of UniqueKeysTracker");
        }
        _telemetrySyncTask.stopScheduledTask();
        _log.info("Successful shutdown of telemetry sync task");
    }
}
