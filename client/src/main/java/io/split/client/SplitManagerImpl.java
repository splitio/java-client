package io.split.client;

import com.google.common.base.Preconditions;
import io.split.client.api.SplitView;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.ParsedSplit;
import io.split.inputValidation.SplitNameValidator;
import io.split.storages.SplitCacheConsumer;
import io.split.telemetry.storage.TelemetryConfigProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import java.util.Optional;
import java.util.concurrent.TimeoutException;

/**
 * Created by adilaijaz on 7/15/16.
 */
public class SplitManagerImpl implements SplitManager {

    private static final Logger _log = LoggerFactory.getLogger(SplitManagerImpl.class);

    private final SplitCacheConsumer _splitCacheConsumer;
    private final SplitClientConfig _config;
    private final SDKReadinessGates _gates;
    private final TelemetryConfigProducer _telemetryConfigProducer;


    public SplitManagerImpl(SplitCacheConsumer splitCacheConsumer,
                            SplitClientConfig config,
                            SDKReadinessGates gates,
                            TelemetryConfigProducer telemetryConfigProducer) {
        _config = Preconditions.checkNotNull(config);
        _splitCacheConsumer = Preconditions.checkNotNull(splitCacheConsumer);
        _gates = Preconditions.checkNotNull(gates);
        _telemetryConfigProducer = telemetryConfigProducer;
    }

    @Override
    public List<SplitView> splits() {
        if (!_gates.isSDKReady()) { {
            _log.warn("splits: the SDK is not ready, results may be incorrect. Make sure to wait for SDK readiness before using this method");
            _telemetryConfigProducer.recordNonReadyUsage();
        }}
        List<SplitView> result = new ArrayList<>();
        Collection<ParsedSplit> parsedSplits = _splitCacheConsumer.getAll();
        for (ParsedSplit split : parsedSplits) {
            result.add(SplitView.fromParsedSplit(split));
        }

        return result;
    }

    @Override
    public SplitView split(String featureFlagName) {
        if (!_gates.isSDKReady()) { {
            _log.warn("split: the SDK is not ready, results may be incorrect. Make sure to wait for SDK readiness before using this method");
            _telemetryConfigProducer.recordNonReadyUsage();
        }}
        Optional<String> result = SplitNameValidator.isValid(featureFlagName, "split");
        if (!result.isPresent()) {
            return null;
        }
        featureFlagName = result.get();

        ParsedSplit parsedSplit = _splitCacheConsumer.get(featureFlagName);
        if (parsedSplit == null) {
            if (_gates.isSDKReady()) {
                _log.warn("split: you passed \"" + featureFlagName + "\" that does not exist in this environment, " +
                        "please double check what feature flags exist in the Split user interface.");
            }
            return null;
        }

        return SplitView.fromParsedSplit(parsedSplit);
    }

    @Override
    public List<String> splitNames() {
        if (!_gates.isSDKReady()) { {
            _log.warn("splitNames: the SDK is not ready, results may be incorrect. Make sure to wait for SDK readiness before using this method");
            _telemetryConfigProducer.recordNonReadyUsage();
        }}
        return _splitCacheConsumer.splitNames();
    }

    @Override
    public void blockUntilReady() throws TimeoutException, InterruptedException {
        if (_config.blockUntilReady() <= 0) {
            throw new IllegalArgumentException("setBlockUntilReadyTimeout must be positive but in config was: " + _config.blockUntilReady());
        }
        if (!_gates.waitUntilInternalReady(_config.blockUntilReady())) {
            _telemetryConfigProducer.recordBURTimeout();
            throw new TimeoutException("SDK was not ready in " + _config.blockUntilReady()+ " milliseconds");
        }
    }
}