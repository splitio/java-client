package io.split.storages.pluggable.adapters;

import io.split.client.utils.Json;
import io.split.engine.experiments.ParsedSplit;
import io.split.storages.SplitCacheProducer;
import io.split.storages.pluggable.CustomStorageWrapper;
import io.split.storages.pluggable.domain.PrefixAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCustomSplitAdapterProducer implements SplitCacheProducer {

    private static final Logger _log = LoggerFactory.getLogger(UserCustomSplitAdapterProducer.class);

    private final CustomStorageWrapper _customStorageWrapper;

    public UserCustomSplitAdapterProducer(CustomStorageWrapper customStorageWrapper) {
        _customStorageWrapper = checkNotNull(customStorageWrapper);
    }


    @Override
    public long getChangeNumber() {
        return Json.fromJson(_customStorageWrapper.get(PrefixAdapter.buildSplitChangeNumber()), Long.class);
    }

    @Override
    public void put(ParsedSplit split) {
        //NoOp
    }

    @Override
    public boolean remove(String splitName) {
        _customStorageWrapper.delete(Stream.of(PrefixAdapter.buildSplitKey(splitName)).collect(Collectors.toList()));
        return false;
    }

    @Override
    public void setChangeNumber(long changeNumber) {
        _customStorageWrapper.set(PrefixAdapter.buildSplitChangeNumber(),Json.toJson(changeNumber));
    }

    @Override
    public void kill(String splitName, String defaultTreatment, long changeNumber) {
        ParsedSplit parsedSplit = Json.fromJson(_customStorageWrapper.get(PrefixAdapter.buildSplitKey(splitName)), ParsedSplit.class);
        if(parsedSplit == null)
            _log.warn("Could not parse Split.");
        _customStorageWrapper.set(PrefixAdapter.buildSplitKey(splitName), Json.toJson(parsedSplit));
    }

    @Override
    public void clear() {
        //NoOp
    }

    @Override
    public void putMany(List<ParsedSplit> splits, long changeNumber) {
        for(ParsedSplit split : splits) {
            _customStorageWrapper.set(PrefixAdapter.buildSplitKey(split.feature()), Json.toJson(split));
        }
        this.setChangeNumber(changeNumber);
    }

    @Override
    public void increaseTrafficType(String trafficType) {
        _customStorageWrapper.increment(PrefixAdapter.buildTrafficTypeExists(trafficType), 1);
    }

    @Override
    public void decreaseTrafficType(String trafficType) {
        _customStorageWrapper.decrement(PrefixAdapter.buildTrafficTypeExists(trafficType), 1);
        _customStorageWrapper.delete(Stream.of(PrefixAdapter.buildTrafficTypeExists(trafficType)).collect(Collectors.toList()));
    }
}
