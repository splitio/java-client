package io.split.storages.pluggable.adapters;

import io.split.client.dtos.RuleBasedSegment;
import io.split.client.utils.Json;
import io.split.engine.experiments.ParsedRuleBasedSegment;
import io.split.engine.experiments.RuleBasedSegmentParser;
import io.split.storages.RuleBasedSegmentCacheConsumer;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.UserStorageWrapper;
import io.split.storages.pluggable.utils.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pluggable.CustomStorageWrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCustomRuleBasedSegmentAdapterConsumer implements RuleBasedSegmentCacheConsumer {

    private static final Logger _log = LoggerFactory.getLogger(UserCustomRuleBasedSegmentAdapterConsumer.class);

    private final RuleBasedSegmentParser _ruleBasedSegmentParser;
    private final UserStorageWrapper _userStorageWrapper;

    public UserCustomRuleBasedSegmentAdapterConsumer(CustomStorageWrapper customStorageWrapper) {
        _ruleBasedSegmentParser = new RuleBasedSegmentParser();
        _userStorageWrapper = new UserStorageWrapper(checkNotNull(customStorageWrapper));
    }

    @Override
    public long getChangeNumber() {
        String wrapperResponse = _userStorageWrapper.get(PrefixAdapter.buildRuleBasedSegmentChangeNumber());
        return Helper.responseToLong(wrapperResponse, -1L);
    }

    @Override
    public ParsedRuleBasedSegment get(String name) {
        String wrapperResponse = _userStorageWrapper.get(PrefixAdapter.buildRuleBasedSegmentKey(name));
        if(wrapperResponse == null) {
            return null;
        }
        RuleBasedSegment ruleBasedSegment = Json.fromJson(wrapperResponse, RuleBasedSegment.class);
        if(ruleBasedSegment == null) {
            _log.warn("Could not parse RuleBasedSegment.");
            return null;
        }
        return _ruleBasedSegmentParser.parse(ruleBasedSegment);
    }

    @Override
    public Collection<ParsedRuleBasedSegment> getAll() {
        Set<String> keys = _userStorageWrapper.getKeysByPrefix(PrefixAdapter.buildGetAllRuleBasedSegment());
        if(keys == null) {
            return new ArrayList<>();
        }
        List<String> wrapperResponse = _userStorageWrapper.getMany(new ArrayList<>(keys));
        if(wrapperResponse == null) {
            return new ArrayList<>();
        }
        return stringsToParsedRuleBasedSegments(wrapperResponse);
    }

    @Override
    public List<String> ruleBasedSegmentNames() {
        Set<String> ruleBasedSegmentNamesWithPrefix = _userStorageWrapper.getKeysByPrefix(PrefixAdapter.buildGetAllRuleBasedSegment());
        ruleBasedSegmentNamesWithPrefix = ruleBasedSegmentNamesWithPrefix.stream().
                map(key -> key.replace(PrefixAdapter.buildRuleBasedSegmentsPrefix(), "")).
                collect(Collectors.toSet());
        return new ArrayList<>(ruleBasedSegmentNamesWithPrefix);
    }

    @Override
    public Set<String> getSegments() {
        return getAll().stream()
                .flatMap(parsedRuleBasedSegment -> parsedRuleBasedSegment.
                        getSegmentsNames().stream()).collect(Collectors.toSet());
    }

    private List<ParsedRuleBasedSegment> stringsToParsedRuleBasedSegments(List<String> elements) {
        List<ParsedRuleBasedSegment> result = new ArrayList<>();
        for(String s : elements) {
            if(s != null) {
                result.add(_ruleBasedSegmentParser.parse(Json.fromJson(s, RuleBasedSegment.class)));
                continue;
            }
            result.add(null);
        }
        return result;
    }
}