package io.split.storages.pluggable.adapters;

import io.split.client.dtos.Split;
import io.split.client.utils.Json;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.experiments.SplitParser;
import io.split.storages.SplitCacheConsumer;
import io.split.storages.pluggable.domain.SafeUserStorageWrapper;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.utils.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pluggable.CustomStorageWrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
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
        return Helper.responseToLong(wrapperResponse, -1L);
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
        Set<String> keys = _safeUserStorageWrapper.getKeysByPrefix(PrefixAdapter.buildGetAllSplit());
        if(keys == null) {
            return new ArrayList<>();
        }
        List<String> wrapperResponse = _safeUserStorageWrapper.getMany(new ArrayList<>(keys));
        if(wrapperResponse == null) {
            return new ArrayList<>();
        }
        return stringsToParsedSplits(wrapperResponse);
    }

    @Override
    public boolean trafficTypeExists(String trafficTypeName) {
        String wrapperResponse = _safeUserStorageWrapper.get(PrefixAdapter.buildTrafficTypeExists(trafficTypeName));
        boolean response = false;
        if(wrapperResponse == null) {
            return response;
        }
        try {
            response = Json.fromJson(wrapperResponse, Boolean.class);
            return response;
        }
        catch(Exception e) {
            _log.info("Error getting boolean from String.");
        }
        return response;
    }

    @Override
    public Collection<ParsedSplit> fetchMany(List<String> names) {
        List<String> wrapperResponse = _safeUserStorageWrapper.getItems(PrefixAdapter.buildFetchManySplits(names));
        if(wrapperResponse == null) {
            return new ArrayList<>();
        }
        return stringsToParsedSplits(wrapperResponse);
    }

    @Override
    public Set<String> getSegments() {
        //NoOp
        return new HashSet<>();
    }

    private List<ParsedSplit> stringsToParsedSplits(List<String> elements) {
        return elements.stream()
                .map(s -> Json.fromJson(s, Split.class))
                .map(_splitParser::parse)
                .collect(Collectors.toList());
    }
}
