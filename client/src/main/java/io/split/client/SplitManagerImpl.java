package io.split.client;

import com.google.common.base.Preconditions;
import io.split.client.api.SplitView;
import io.split.client.dtos.Partition;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.experiments.SplitFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * Created by adilaijaz on 7/15/16.
 */
public class SplitManagerImpl implements SplitManager {

    private static final Logger _log = LoggerFactory.getLogger(SplitManagerImpl.class);

    private final SplitFetcher _splitFetcher;
    private final SplitClientConfig _config;
    private final SDKReadinessGates _gates;


    public SplitManagerImpl(SplitFetcher splitFetcher,
                            SplitClientConfig config,
                            SDKReadinessGates gates) {
        _config = Preconditions.checkNotNull(config);
        _splitFetcher  = Preconditions.checkNotNull(splitFetcher);
        _gates = Preconditions.checkNotNull(gates);
    }

    @Override
    public List<SplitView> splits() {
        List<SplitView> result = new ArrayList<>();
        List<ParsedSplit> parsedSplits = _splitFetcher.fetchAll();
        for (ParsedSplit split : parsedSplits) {
            result.add(toSplitView(split));
        }
        return result;
    }

    @Override
    public SplitView split(String featureName) {
        if (featureName == null) {
            _log.error("split: you passed a null split name, split name must be a non-empty string");
            return null;
        }
        if (featureName.isEmpty()) {
            _log.error("split: you passed an empty split name, split name must be a non-empty string");
            return null;
        }
        ParsedSplit parsedSplit = _splitFetcher.fetch(featureName);
        if (parsedSplit == null) {
            if (_gates.isSDKReadyNow()) {
                _log.error("split: you passed \"" + featureName + "\" that does not exist in this environment, " +
                        "please double check what Splits exist in the web console.");
            }
            return null;
        }
        return toSplitView(parsedSplit);
    }

    @Override
    public List<String> splitNames() {
        List<String> result = new ArrayList<>();
        List<ParsedSplit> parsedSplits = _splitFetcher.fetchAll();
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

    private SplitView toSplitView(ParsedSplit parsedSplit) {
        SplitView splitView = new SplitView();
        splitView.name = parsedSplit.feature();
        splitView.trafficType = parsedSplit.trafficTypeName();
        splitView.killed = parsedSplit.killed();
        splitView.changeNumber = parsedSplit.changeNumber();

        Set<String> treatments = new HashSet<String>();
        for (ParsedCondition condition : parsedSplit.parsedConditions()) {
            for (Partition partition : condition.partitions()) {
                treatments.add(partition.treatment);
            }
        }
        treatments.add(parsedSplit.defaultTreatment());

        splitView.treatments = new ArrayList<String>(treatments);

        return splitView;
    }
}
