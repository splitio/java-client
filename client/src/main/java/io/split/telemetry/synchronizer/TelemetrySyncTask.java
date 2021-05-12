package io.split.telemetry.synchronizer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class TelemetrySyncTask {

    private final ScheduledExecutorService _telemetrySyncScheduledExecutorService;
    private final TelemetrySynchronizer _telemetrySynchronizer;
    private final int _telemetryRefreshRate;

    public TelemetrySyncTask(int telemetryRefreshRate, TelemetrySynchronizer telemetrySynchronizer) {
        ThreadFactory telemetrySyncThreadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Telemetry-sync-%d")
                .build();
        _telemetrySynchronizer = telemetrySynchronizer;
        _telemetryRefreshRate = telemetryRefreshRate;
        _telemetrySyncScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(telemetrySyncThreadFactory);
        try {
            this.startScheduledTask();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @VisibleForTesting
    protected void startScheduledTask() throws Exception {
        _telemetrySyncScheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                _telemetrySynchronizer.synchronizeStats();
            } catch (Exception e) {
                e.printStackTrace();
            }
        },0l,  _telemetryRefreshRate, TimeUnit.SECONDS);
    }

    public void stopScheduledTask() {
        try {
            _telemetrySynchronizer.synchronizeStats();
        } catch (Exception e) {
            e.printStackTrace();
        }
        _telemetrySyncScheduledExecutorService.shutdown();
    }
}
