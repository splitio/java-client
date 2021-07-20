package io.split.storages.pluggable.adapters;

import io.split.client.utils.Json;
import io.split.engine.experiments.ParsedSplit;
import io.split.storages.SplitCacheConsumer;
import io.split.storages.pluggable.CustomStorageWrapper;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.RawParsedSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCustomSplitAdapterConsumer  implements SplitCacheConsumer {

    private static final Logger _log = LoggerFactory.getLogger(UserCustomSplitAdapterConsumer.class);

    private final CustomStorageWrapper _customStorageWrapper;

    public UserCustomSplitAdapterConsumer(CustomStorageWrapper customStorageWrapper) {
        _customStorageWrapper = checkNotNull(customStorageWrapper);
    }

    @Override
    public long getChangeNumber() {
        return Json.fromJson(_customStorageWrapper.get(PrefixAdapter.buildSplitChangeNumber()), Long.class);
    }

    @Override
    public ParsedSplit get(String name) {
        RawParsedSplit parsedSplit = Json.fromJson(_customStorageWrapper.get(PrefixAdapter.buildSplitKey(name)), RawParsedSplit.class);
        if(parsedSplit == null)
            _log.warn("Could not parse Split.");
        return null;
    }

    @Override
    public Collection<ParsedSplit> getAll() {
        List<ParsedSplit> splits = Json.fromJson(_customStorageWrapper.get(PrefixAdapter.buildGetAllSplit()), List.class);
        if(splits == null)
            _log.warn("Could not parse Splits.");
        return splits;
    }

    @Override
    public boolean trafficTypeExists(String trafficTypeName) {
        boolean splits = Json.fromJson(_customStorageWrapper.get(PrefixAdapter.buildTrafficTypeExists(trafficTypeName)), Boolean.class);
        return splits;
    }

    @Override
    public Collection<ParsedSplit> fetchMany(List<String> names) {
        List<ParsedSplit> splits = Json.fromJson(_customStorageWrapper.getItems(Json.toJson(PrefixAdapter.buildFetchManySplits(names))), List.class);
        if(splits == null)
            _log.warn("Could not parse Splits.");
        return splits;
    }
}
