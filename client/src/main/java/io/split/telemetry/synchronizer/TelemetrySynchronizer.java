package io.split.telemetry.synchronizer;

import io.split.client.SplitClientConfig;

import java.util.List;
import java.util.Map;

public interface TelemetrySynchronizer {
    void synchronizeConfig(SplitClientConfig config, long timeUntilReady, Map<String, Long> factoryInstances, List<String> tags);
    void synchronizeStats() throws Exception;
}
