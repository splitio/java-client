package io.split.storages.pluggable.adapters;

import io.split.client.dtos.Split;
import io.split.client.utils.Json;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.experiments.SplitParser;
import io.split.storages.SplitCacheConsumer;
import io.split.storages.pluggable.CustomStorageWrapper;
import io.split.storages.pluggable.domain.PrefixAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCustomSplitAdapterConsumer  implements SplitCacheConsumer {

    private static final Logger _log = LoggerFactory.getLogger(UserCustomSplitAdapterConsumer.class);

    private final CustomStorageWrapper _customStorageWrapper;
    private final SplitParser _splitParser;

    public UserCustomSplitAdapterConsumer(CustomStorageWrapper customStorageWrapper) {
        _customStorageWrapper = checkNotNull(customStorageWrapper);
        _splitParser = new SplitParser();
    }

    @Override
    public long getChangeNumber() {
        return Json.fromJson(_customStorageWrapper.get(PrefixAdapter.buildSplitChangeNumber()), Long.class);
    }

    @Override
    public ParsedSplit get(String name) {
        Split split = Json.fromJson(_customStorageWrapper.get(PrefixAdapter.buildSplitKey(name)), Split.class);
        if(split == null) {
            _log.warn("Could not parse Split.");
            return null;
        }
        return _splitParser.parse(split);
    }

    @Override
    public Collection<ParsedSplit> getAll() {
        List<Split> splits = Json.fromJsonToArray(_customStorageWrapper.get(PrefixAdapter.buildGetAllSplit()), Split[].class);
        if(splits == null || splits.size() == 0) {
            _log.warn("Could not parse Splits.");
            return new ArrayList<>();
        }
        return splits.stream().map(s -> _splitParser.parse(s)).collect(Collectors.toList());
    }

    @Override
    public boolean trafficTypeExists(String trafficTypeName) {
        boolean splits = Json.fromJson(_customStorageWrapper.get(PrefixAdapter.buildTrafficTypeExists(trafficTypeName)), Boolean.class);
        return splits;
    }

    @Override
    public Collection<ParsedSplit> fetchMany(List<String> names) {
        List<Split> splits = Json.fromJsonToArray(_customStorageWrapper.getItems(PrefixAdapter.buildFetchManySplits(names)), Split[].class);
        if(splits == null || splits.size() == 0) {
            _log.warn("Could not parse Splits.");
            return new ArrayList<>();
        }
        return splits.stream().map(s -> _splitParser.parse(s)).collect(Collectors.toList());
    }

    @Override
    public Set<String> getSegments() {
        //NoOp
        return new HashSet<>();
    }
}
