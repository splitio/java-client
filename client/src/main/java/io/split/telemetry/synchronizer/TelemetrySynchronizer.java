package io.split.telemetry.synchronizer;

import io.split.telemetry.domain.InitConfig;

import java.util.List;
import java.util.Map;

public interface TelemetrySynchronizer {
    void synchronizeConfig(InitConfig config, long timedUntilReady, Map<String, Long> factoryInstances, List<String> tags);
    void synchronizeStats() throws Exception;
}
