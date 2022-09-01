package io.split.telemetry.synchronizer;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class TelemetrySyncTask {

    private static final Logger _log = LoggerFactory.getLogger(TelemetrySyncTask.class);
    private final ScheduledExecutorService _telemetrySyncScheduledExecutorService;
    private final TelemetrySynchronizer _telemetrySynchronizer;
    private final int _telemetryRefreshRate;

    public TelemetrySyncTask(int telemetryRefreshRate, TelemetrySynchronizer telemetrySynchronizer) {
        ThreadFactory telemetrySyncThreadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Telemetry-sync-%d")
                .build();
        _telemetrySynchronizer = checkNotNull(telemetrySynchronizer);
        _telemetryRefreshRate = telemetryRefreshRate;
        _telemetrySyncScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(telemetrySyncThreadFactory);
    }

    public void startScheduledTask() {
        _telemetrySyncScheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                _telemetrySynchronizer.synchronizeStats();
            } catch (Exception e) {
                _log.warn("Error sending telemetry stats.");
            }
        },_telemetryRefreshRate,  _telemetryRefreshRate, TimeUnit.SECONDS);
    }

    public void stopScheduledTask(long splitCount, long segmentCount, long segmentKeyCount) {
        try {
            _telemetrySynchronizer.finalSynchronization(splitCount, segmentCount, segmentKeyCount);
        } catch (Exception e) {
            _log.warn("Error trying to send telemetry stats.");
        }
        _telemetrySyncScheduledExecutorService.shutdown();
    }

}
