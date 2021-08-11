package io.split.storages.pluggable.adapters;

import io.split.client.dtos.Split;
import io.split.client.utils.Json;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.experiments.SplitParser;
import io.split.storages.SplitCacheConsumer;
import io.split.storages.pluggable.CustomStorageWrapper;
import io.split.storages.pluggable.domain.SafeUserStorageWrapper;
import io.split.storages.pluggable.domain.PrefixAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCustomSplitAdapterConsumer  implements SplitCacheConsumer {

    private static final Logger _log = LoggerFactory.getLogger(UserCustomSplitAdapterConsumer.class);

    private final SplitParser _splitParser;
    private final SafeUserStorageWrapper _safeUserStorageWrapper;

    public UserCustomSplitAdapterConsumer(CustomStorageWrapper customStorageWrapper) {
        _splitParser = new SplitParser();
        _safeUserStorageWrapper = new SafeUserStorageWrapper(checkNotNull(customStorageWrapper));
    }

    @Override
    public long getChangeNumber() {
        String wrapperResponse = _safeUserStorageWrapper.get(PrefixAdapter.buildSplitChangeNumber());
        if(wrapperResponse == null) {
            return 0L;
        }
        return Json.fromJson(wrapperResponse, Long.class);
    }

    @Override
    public ParsedSplit get(String name) {
        String wrapperResponse = _safeUserStorageWrapper.get(PrefixAdapter.buildSplitKey(name));
        if(wrapperResponse == null) {
            return null;
        }
        Split split = Json.fromJson(wrapperResponse, Split.class);
        if(split == null) {
            _log.warn("Could not parse Split.");
            return null;
        }
        return _splitParser.parse(split);
    }

    @Override
    public Collection<ParsedSplit> getAll() {
        String wrapperResponse = _safeUserStorageWrapper.get(PrefixAdapter.buildGetAllSplit());
        if(wrapperResponse == null) {
            return new ArrayList<>();
        }
        List<Split> splits = Json.fromJsonToArray(wrapperResponse, Split[].class);
        if(splits.size() == 0) {
            _log.warn("Could not parse Splits.");
            return new ArrayList<>();
        }
        return splits.stream().map(_splitParser::parse).collect(Collectors.toList());
    }

    @Override
    public boolean trafficTypeExists(String trafficTypeName) {
        String wrapperResponse = _safeUserStorageWrapper.get(PrefixAdapter.buildTrafficTypeExists(trafficTypeName));
        if(wrapperResponse == null) {
            return false;
        }
        return Json.fromJson(wrapperResponse, Boolean.class);
    }

    @Override
    public Collection<ParsedSplit> fetchMany(List<String> names) {
        String wrapperResponse = _safeUserStorageWrapper.getItems(PrefixAdapter.buildFetchManySplits(names));
        if(wrapperResponse == null) {
            return new ArrayList<>();
        }
        List<Split> splits = Json.fromJsonToArray(wrapperResponse, Split[].class);
        if(splits.size() == 0) {
            _log.warn("Could not parse Splits.");
            return new ArrayList<>();
        }
        return splits.stream().map(_splitParser::parse).collect(Collectors.toList());
    }

    @Override
    public Set<String> getSegments() {
        //NoOp
        return new HashSet<>();
    }
}
