package io.split.client;

import com.google.common.base.Preconditions;
import io.split.client.api.SplitView;
import io.split.client.dtos.Partition;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.experiments.SplitFetcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * Created by adilaijaz on 7/15/16.
 */
public class SplitManagerImpl implements SplitManager {

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
        ParsedSplit parsedSplit = _splitFetcher.fetch(featureName);
        return parsedSplit == null ? null : toSplitView(parsedSplit);
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
        if (_config.blockUntilReady() > 0) {
            if (!_gates.isSDKReady(_config.blockUntilReady())) {
                throw new TimeoutException("SDK was not ready in " + _config.blockUntilReady()+ " milliseconds");
            }
        } else {
            throw new IllegalArgumentException("waitInMilliseconds must be positive in config was set: " + _config.blockUntilReady());
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
