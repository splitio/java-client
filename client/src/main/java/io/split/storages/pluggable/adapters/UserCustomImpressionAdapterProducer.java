package io.split.storages.pluggable.adapters;

import io.split.client.dtos.KeyImpression;
import io.split.client.impressions.ImpressionsStorageProducer;
import io.split.client.utils.Json;
import io.split.storages.pluggable.CustomStorageWrapper;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.SafeUserStorageWrapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCustomImpressionAdapterProducer implements ImpressionsStorageProducer {

    private final SafeUserStorageWrapper _safeUserStorageWrapper;

    public UserCustomImpressionAdapterProducer(CustomStorageWrapper customStorageWrapper) {
        _safeUserStorageWrapper = new SafeUserStorageWrapper(checkNotNull(customStorageWrapper));
    }
    @Override
    public boolean put(KeyImpression imps) {
        return this.put(Collections.singletonList(imps));
    }

    @Override
    public boolean put(List<KeyImpression> imps) {
        List<String> impressions = imps.stream().map(keyImp -> Json.toJson(keyImp)).collect(Collectors.toList());
        _safeUserStorageWrapper.pushItems(PrefixAdapter.buildImpressions(), impressions);
        return true;
    }
}
