package io.split.telemetry.synchronizer;

import io.split.client.ApiKeyCounter;
import io.split.client.SplitClientConfig;
import io.split.engine.SDKReadinessGates;

import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkNotNull;

public class TelemetryConfigInitializer {

    private final TelemetrySynchronizer _telemetrySynchronizer;
    private final SDKReadinessGates _gates;
    private final SplitClientConfig _config;

    public TelemetryConfigInitializer(TelemetrySynchronizer _telemetrySynchronizer, SDKReadinessGates _gates, SplitClientConfig config) {
        this._telemetrySynchronizer = checkNotNull(_telemetrySynchronizer);
        this._gates = checkNotNull(_gates);
        _config = checkNotNull(config);
        this.waitForSDKReady();
    }

    private void waitForSDKReady() {
        long initTime = System.currentTimeMillis();
        while(true) {
            if (_gates.isSDKReadyNow()) {
                _telemetrySynchronizer.synchronizeConfig(_config,System.currentTimeMillis()-initTime, ApiKeyCounter.getApiKeyCounterInstance().getFactoryInstances(),new ArrayList<>());
                break;
            }
        }
    }
}
