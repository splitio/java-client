package io.split.client;

import io.split.client.api.SplitView;
import io.split.client.dtos.Partition;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.experiments.SplitFetcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by adilaijaz on 7/15/16.
 */
public class SplitManagerImpl implements SplitManager {

    private final SplitFetcher _splitFetcher;


    public SplitManagerImpl(SplitFetcher splitFetcher) {
        _splitFetcher  = splitFetcher;
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
