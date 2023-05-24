package io.split.storages.pluggable.adapters;

import io.split.client.dtos.Split;
import io.split.client.utils.Json;
import io.split.engine.experiments.ParsedSplit;
import io.split.storages.SplitCacheProducer;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.UserStorageWrapper;
import io.split.storages.pluggable.utils.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pluggable.CustomStorageWrapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCustomSplitAdapterProducer implements SplitCacheProducer {

    private static final Logger _log = LoggerFactory.getLogger(UserCustomSplitAdapterProducer.class);

    private final UserStorageWrapper _userStorageWrapper;

    public UserCustomSplitAdapterProducer(CustomStorageWrapper customStorageWrapper) {
        _userStorageWrapper = new UserStorageWrapper(checkNotNull(customStorageWrapper));
    }

    @Override
    public long getChangeNumber() {
        String wrapperResponse = _userStorageWrapper.get(PrefixAdapter.buildSplitChangeNumber());
        return Helper.responseToLong(wrapperResponse, -1L);
    }

    @Override
    public boolean remove(String splitName) {
        String wrapperResponse = _userStorageWrapper.get(PrefixAdapter.buildSplitKey(splitName));
        if(wrapperResponse == null) {
            return false;
        }
        Split split = Json.fromJson(wrapperResponse, Split.class);
        if(split == null) {
            _log.info("Could not parse Split.");
            return false;
        }
        _userStorageWrapper.delete(Stream.of(PrefixAdapter.buildSplitKey(splitName)).collect(Collectors.toList()));
         if(split.trafficTypeName != null){
             this.decreaseTrafficType(split.trafficTypeName);
         }
        return true;
    }

    @Override
    public void setChangeNumber(long changeNumber) {
        _userStorageWrapper.set(PrefixAdapter.buildSplitChangeNumber(),Json.toJson(changeNumber));
    }

    @Override
    public void kill(String splitName, String defaultTreatment, long changeNumber) {
        String wrapperResponse = _userStorageWrapper.get(PrefixAdapter.buildSplitKey(splitName));
        if(wrapperResponse == null) {
            return;
        }
        Split split = Json.fromJson(wrapperResponse, Split.class);
        if(split == null) {
            _log.info("Could not parse Split.");
            return;
        }
        _userStorageWrapper.set(PrefixAdapter.buildSplitKey(splitName), Json.toJson(split));
    }

    @Override
    public void clear() {
        //NoOp
    }

    @Override
    public void putMany(List<ParsedSplit> splits) {
        for(ParsedSplit split : splits) {
            _userStorageWrapper.set(PrefixAdapter.buildSplitKey(split.feature()), Json.toJson(split));
            this.increaseTrafficType(PrefixAdapter.buildTrafficTypeExists(split.trafficTypeName()));
        }
    }

    @Override
    public void increaseTrafficType(String trafficType) {
        _userStorageWrapper.increment(PrefixAdapter.buildTrafficTypeExists(trafficType), 1);
    }

    @Override
    public void decreaseTrafficType(String trafficType) {
        long trafficTypeCount = _userStorageWrapper.decrement(PrefixAdapter.buildTrafficTypeExists(trafficType), 1);
        if(trafficTypeCount<=0) {
            _userStorageWrapper.delete(Stream.of(PrefixAdapter.buildTrafficTypeExists(trafficType)).collect(Collectors.toList()));
        }
    }

    @Override
    public void updateFeatureFlag(ParsedSplit parsedSplit) {
        List<ParsedSplit> parsedSplits = new ArrayList<>();
        parsedSplits.add(parsedSplit);
        putMany(parsedSplits);
    }

    @Override
    public Set<String> getSegments() {
        //NoOp
        return new HashSet<>();
    }
}
