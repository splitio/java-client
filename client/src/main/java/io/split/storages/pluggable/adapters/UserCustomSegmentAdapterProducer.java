package io.split.storages.pluggable.adapters;

import io.split.client.utils.Json;
import io.split.storages.SegmentCacheProducer;
import io.split.storages.pluggable.CustomStorageWrapper;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.SafeUserStorageWrapper;
import io.split.storages.pluggable.utils.Helper;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCustomSegmentAdapterProducer implements SegmentCacheProducer {

    private final SafeUserStorageWrapper _safeUserStorageWrapper;

    public UserCustomSegmentAdapterProducer(CustomStorageWrapper customStorageWrapper) {
        _safeUserStorageWrapper = new SafeUserStorageWrapper(checkNotNull(customStorageWrapper));
    }
    @Override
    public long getChangeNumber(String segmentName) {
        String wrapperResponse = _safeUserStorageWrapper.get(PrefixAdapter.buildSegment(segmentName));
        return Helper.responseToLong(wrapperResponse, -1L);
    }

    @Override
    public void updateSegment(String segmentName, List<String> toAdd, List<String> toRemove, long changeNumber) {
        String keySegment = PrefixAdapter.buildSegment(segmentName);
        _safeUserStorageWrapper.addItems(keySegment, toAdd);
        _safeUserStorageWrapper.removeItems(keySegment, toRemove);
        _safeUserStorageWrapper.set(PrefixAdapter.buildSegmentTill(segmentName), Json.toJson(changeNumber));
    }

    @Override
    public void setChangeNumber(String segmentName, long changeNumber) {
        _safeUserStorageWrapper.set(PrefixAdapter.buildSegmentTill(segmentName), Json.toJson(changeNumber));
    }
}
