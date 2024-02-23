package io.split.storages.pluggable.synchronizer;

import com.google.common.annotations.VisibleForTesting;
import io.split.client.SplitClientConfig;
import io.split.client.dtos.UniqueKeys;
import io.split.client.utils.Json;
import io.split.client.utils.SDKMetadata;
import io.split.storages.enums.OperationMode;
import io.split.storages.pluggable.domain.ConfigConsumer;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.UserStorageWrapper;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
import pluggable.CustomStorageWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class TelemetryConsumerSubmitter implements TelemetrySynchronizer {

    private static final String STORAGE = "PLUGGABLE";

    private final UserStorageWrapper _userStorageWrapper;
    private final SDKMetadata _sdkMetadata;

    public TelemetryConsumerSubmitter(CustomStorageWrapper customStorageWrapper, SDKMetadata sdkMetadata) {
        _userStorageWrapper = new UserStorageWrapper(checkNotNull(customStorageWrapper));
        _sdkMetadata = checkNotNull(sdkMetadata);
    }

    @Override
    public void synchronizeConfig(SplitClientConfig config, long timeUntilReady, Map<String, Long> factoryInstances, List<String> tags) {
        String key = String.format("%s/%s/%s", _sdkMetadata.getSdkVersion(), _sdkMetadata.getMachineName(), _sdkMetadata.getMachineIp());
        _userStorageWrapper.hSet(PrefixAdapter.buildTelemetryInit(), key, Json.toJson(generateConfig(config, factoryInstances, tags)));
    }

    @Override
    public void synchronizeStats() {
        //No-op
    }

    @Override
    public void synchronizeUniqueKeys(UniqueKeys uniqueKeys) {
        List<String> uniqueKeysToSend;
        for (UniqueKeys.UniqueKey uniqueKey: uniqueKeys.uniqueKeys) {
            uniqueKeysToSend = new ArrayList<>(Arrays.asList(Json.toJson(uniqueKey)));
            _userStorageWrapper.pushItems(PrefixAdapter.buildUniqueKeys(), uniqueKeysToSend);
        }
    }

    @Override
    public void finalSynchronization() {
        //No-Op
    }

    @VisibleForTesting
    ConfigConsumer generateConfig(SplitClientConfig splitClientConfig, Map<String, Long> factoryInstances, List<String> tags) {
        ConfigConsumer config = new ConfigConsumer();
        config.setOperationMode(splitClientConfig.operationMode()== OperationMode.STANDALONE ? 0 : 1);
        config.setStorage(STORAGE);
        config.setActiveFactories(factoryInstances.size());
        config.setRedundantFactories(getRedundantFactories(factoryInstances));
        config.setTags(tags.size() < 10 ? tags : tags.subList(0, 10));
        return config;
    }

    private long getRedundantFactories(Map<String, Long> factoryInstances) {
        return factoryInstances.values().stream().mapToLong(l ->  l - 1L).sum();
    }
}