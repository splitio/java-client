package io.split.client;

import com.google.common.base.Preconditions;
import io.split.client.api.SplitView;
import io.split.engine.SDKReadinessGates;
import io.split.cache.SplitCache;
import io.split.engine.experiments.ParsedSplit;
import io.split.inputValidation.SplitNameValidator;
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

    private final SplitCache _splitCache;
    private final SplitClientConfig _config;
    private final SDKReadinessGates _gates;


    public SplitManagerImpl(SplitCache splitCache,
                            SplitClientConfig config,
                            SDKReadinessGates gates) {
        _config = Preconditions.checkNotNull(config);
        _splitCache  = Preconditions.checkNotNull(splitCache);
        _gates = Preconditions.checkNotNull(gates);
    }

    @Override
    public List<SplitView> splits() {
        List<SplitView> result = new ArrayList<>();
        Collection<ParsedSplit> parsedSplits = _splitCache.getAll();
        for (ParsedSplit split : parsedSplits) {
            result.add(SplitView.fromParsedSplit(split));
        }

        return result;
    }

    @Override
    public SplitView split(String featureName) {
        Optional<String> result = SplitNameValidator.isValid(featureName, "split");
        if (!result.isPresent()) {
            return null;
        }
        featureName = result.get();

        ParsedSplit parsedSplit = _splitCache.get(featureName);
        if (parsedSplit == null) {
            if (_gates.isSDKReadyNow()) {
                _log.warn("split: you passed \"" + featureName + "\" that does not exist in this environment, " +
                        "please double check what Splits exist in the web console.");
            }
            return null;
        }

        return SplitView.fromParsedSplit(parsedSplit);
    }

    @Override
    public List<String> splitNames() {
        List<String> result = new ArrayList<>();
        Collection<ParsedSplit> parsedSplits = _splitCache.getAll();
        for (ParsedSplit split : parsedSplits) {
            result.add(split.feature());
        }

        return result;
    }

    @Override
    public void blockUntilReady() throws TimeoutException, InterruptedException {
        if (_config.blockUntilReady() <= 0) {
            throw new IllegalArgumentException("setBlockUntilReadyTimeout must be positive but in config was: " + _config.blockUntilReady());
        }
        if (!_gates.isSDKReady(_config.blockUntilReady())) {
            throw new TimeoutException("SDK was not ready in " + _config.blockUntilReady()+ " milliseconds");
        }
    }
}
