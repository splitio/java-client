package io.split.storages.pluggable.synchronizer;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.split.client.SplitClientConfig;
import io.split.client.dtos.UniqueKeys;
import io.split.client.utils.Json;
import io.split.client.utils.SDKMetadata;
import io.split.storages.enums.OperationMode;
import io.split.storages.pluggable.domain.ConfigConsumer;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.SafeUserStorageWrapper;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
import pluggable.CustomStorageWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class TelemetryConsumerSubmitter implements TelemetrySynchronizer {

    private static final String STORAGE = "PLUGGABLE";

    private final SafeUserStorageWrapper _safeUserStorageWrapper;
    private final SDKMetadata _sdkMetadata;
    private final Gson _json = new GsonBuilder().create();

    public TelemetryConsumerSubmitter(CustomStorageWrapper customStorageWrapper, SDKMetadata sdkMetadata) {
        _safeUserStorageWrapper = new SafeUserStorageWrapper(checkNotNull(customStorageWrapper));
        _sdkMetadata = checkNotNull(sdkMetadata);
    }

    @Override
    public void synchronizeConfig(SplitClientConfig config, long timeUntilReady, Map<String, Long> factoryInstances, List<String> tags) {
        _safeUserStorageWrapper.set(PrefixAdapter.buildTelemetryInit(_sdkMetadata.getSdkVersion(), _sdkMetadata.getMachineIp(), _sdkMetadata.getMachineName()), Json.toJson(generateConfig(config, factoryInstances, tags)));
    }

    @Override
    public void synchronizeStats() {
        //No-op
    }

    @Override
    public void synchronizeUniqueKeys(UniqueKeys uniqueKeys) {
        List<String> uniqueKeysToSend = new ArrayList<>(Arrays.asList(_json.toJson(uniqueKeys)));
        _safeUserStorageWrapper.pushItems(PrefixAdapter.buildUniqueKeys(), uniqueKeysToSend);
    }

    @Override
    public void finalSynchronization(long splitCount, long segmentCount, long segmentKeyCount) {
        //No-Op
    }

    @VisibleForTesting
    ConfigConsumer generateConfig(SplitClientConfig splitClientConfig, Map<String, Long> factoryInstances, List<String> tags) {
        ConfigConsumer config = new ConfigConsumer();
        config.set_operationMode(splitClientConfig.operationMode()== OperationMode.STANDALONE ? 0 : 1);
        config.set_storage(STORAGE);
        config.set_activeFactories(factoryInstances.size());
        config.set_redundantFactories(getRedundantFactories(factoryInstances));
        config.set_tags(tags.size() < 10 ? tags : tags.subList(0, 10));
        return config;
    }

    private long getRedundantFactories(Map<String, Long> factoryInstances) {
        return factoryInstances.values().stream().mapToLong(l ->  l - 1L).sum();
    }
}
