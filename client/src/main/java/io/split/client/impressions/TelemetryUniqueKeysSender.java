package io.split.client.impressions;

import io.split.client.dtos.UniqueKeys;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;

public class TelemetryUniqueKeysSender implements UniqueKeysSender {

    private final TelemetrySynchronizer _telemetrySynchronizer;

    public TelemetryUniqueKeysSender(TelemetrySynchronizer telemetrySynchronizer) {
        _telemetrySynchronizer = telemetrySynchronizer;
    }

    @Override
    public void send(UniqueKeys uniqueKeys) {
        _telemetrySynchronizer.synchronizeUniqueKeys(uniqueKeys);
    }
}
