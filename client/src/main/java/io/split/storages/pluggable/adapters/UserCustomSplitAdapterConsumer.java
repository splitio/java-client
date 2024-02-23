package io.split.storages.pluggable.adapters;

import io.split.client.dtos.Split;
import io.split.client.utils.Json;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.experiments.SplitParser;
import io.split.storages.SplitCacheConsumer;
import io.split.storages.pluggable.domain.UserPipelineWrapper;
import io.split.storages.pluggable.domain.UserStorageWrapper;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.utils.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pluggable.CustomStorageWrapper;
import pluggable.Result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCustomSplitAdapterConsumer  implements SplitCacheConsumer {

    private static final Logger _log = LoggerFactory.getLogger(UserCustomSplitAdapterConsumer.class);

    private final SplitParser _splitParser;
    private final UserStorageWrapper _userStorageWrapper;

    public UserCustomSplitAdapterConsumer(CustomStorageWrapper customStorageWrapper) {
        _splitParser = new SplitParser();
        _userStorageWrapper = new UserStorageWrapper(checkNotNull(customStorageWrapper));
    }

    @Override
    public long getChangeNumber() {
        String wrapperResponse = _userStorageWrapper.get(PrefixAdapter.buildSplitChangeNumber());
        return Helper.responseToLong(wrapperResponse, -1L);
    }

    @Override
    public ParsedSplit get(String name) {
        String wrapperResponse = _userStorageWrapper.get(PrefixAdapter.buildSplitKey(name));
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
        Set<String> keys = _userStorageWrapper.getKeysByPrefix(PrefixAdapter.buildGetAllSplit());
        if(keys == null) {
            return new ArrayList<>();
        }
        List<String> wrapperResponse = _userStorageWrapper.getMany(new ArrayList<>(keys));
        if(wrapperResponse == null) {
            return new ArrayList<>();
        }
        return stringsToParsedSplits(wrapperResponse);
    }

    @Override
    public boolean trafficTypeExists(String trafficTypeName) {
        String wrapperResponse = _userStorageWrapper.get(PrefixAdapter.buildTrafficTypeExists(trafficTypeName));
        if(wrapperResponse == null) {
            return false;
        }
        try {
            Long value = Json.fromJson(wrapperResponse, Long.class);
            return value != null && value > 0;
        }
        catch(Exception e) {
            _log.info("Error getting boolean from String.");
        }
        return false;
    }

    @Override
    public List<String> splitNames() {
        Set<String> splitNamesWithPrefix = _userStorageWrapper.getKeysByPrefix(PrefixAdapter.buildGetAllSplit());
        splitNamesWithPrefix = splitNamesWithPrefix.stream().map(key -> key.replace(PrefixAdapter.buildSplitsPrefix(), "")).
                collect(Collectors.toSet());
        return new ArrayList<>(splitNamesWithPrefix);
    }

    @Override
    public Map<String, HashSet<String>> getNamesByFlagSets(List<String> flagSets) {
        Map<String, HashSet<String>> toReturn = new HashMap<>();
        try {
            if (flagSets == null) {
                return toReturn;
            }
            UserPipelineWrapper pipelineExecution = _userStorageWrapper.pipeline();
            for (String set: flagSets) {
                 pipelineExecution.getMembers(PrefixAdapter.buildFlagSetPrefix(set));
            }
            List<Result> results = pipelineExecution.exec();
            if (results == null || results.isEmpty()){
                return toReturn;
            }
            for (int i = 0; i < results.size(); i ++) {
                Optional<HashSet<String>> featureFlags = results.get(i).asHash();
                if(featureFlags.isPresent()) {
                    toReturn.put(flagSets.get(i), featureFlags.get());
                }
            }
        } catch (Exception e) {
            _log.warn("Redis pipeline exception when getting names by flag sets: ", e);
        }
        return toReturn;
    }

    @Override
    public Map<String, ParsedSplit> fetchMany(List<String> names) {
        Map<String, ParsedSplit> result = new HashMap<>();
        List<String> wrapperResponse = _userStorageWrapper.getItems(PrefixAdapter.buildFetchManySplits(names));
        if(wrapperResponse == null) {
            return result;
        }
        List<ParsedSplit> parsedSplits = stringsToParsedSplits(wrapperResponse);
        for(int i=0; i < parsedSplits.size(); i++) {
            result.put(names.get(i), parsedSplits.get(i));
        }
        return result;
    }

    @Override
    public Set<String> getSegments() {
        //NoOp
        return new HashSet<>();
    }

    private List<ParsedSplit> stringsToParsedSplits(List<String> elements) {
        List<ParsedSplit> result = new ArrayList<>();
        for(String s : elements) {
            if(s != null) {
                result.add(_splitParser.parse(Json.fromJson(s, Split.class)));
                continue;
            }
            result.add(null);
        }
        return result;
    }
}