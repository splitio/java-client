package io.split.telemetry.synchronizer;

import io.split.client.SplitClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.split.client.utils.SplitExecutorFactory.buildSingleThreadScheduledExecutor;

public class TelemetrySyncTask {

    private static final Logger _log = LoggerFactory.getLogger(TelemetrySyncTask.class);
    private final ScheduledExecutorService _telemetrySyncScheduledExecutorService;
    private final TelemetrySynchronizer _telemetrySynchronizer;
    private final int _telemetryRefreshRate;

    public TelemetrySyncTask(int telemetryRefreshRate, TelemetrySynchronizer telemetrySynchronizer, SplitClientConfig config) {
        _telemetrySynchronizer = checkNotNull(telemetrySynchronizer);
        _telemetryRefreshRate = telemetryRefreshRate;
        _telemetrySyncScheduledExecutorService = buildSingleThreadScheduledExecutor(config.getThreadFactory(), "Telemetry-sync-%d");
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

    public void stopScheduledTask() {
        try {
            _telemetrySynchronizer.finalSynchronization();
        } catch (Exception e) {
            _log.warn("Error trying to send telemetry stats.");
        }
        _telemetrySyncScheduledExecutorService.shutdown();
    }

}