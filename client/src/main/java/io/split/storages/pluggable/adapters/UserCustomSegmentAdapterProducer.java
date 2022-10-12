package io.split.storages.pluggable.adapters;

import io.split.client.utils.Json;
import io.split.storages.SegmentCacheProducer;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.UserStorageWrapper;
import io.split.storages.pluggable.utils.Helper;
import pluggable.CustomStorageWrapper;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCustomSegmentAdapterProducer implements SegmentCacheProducer {

    private final UserStorageWrapper _userStorageWrapper;

    public UserCustomSegmentAdapterProducer(CustomStorageWrapper customStorageWrapper) {
        _userStorageWrapper = new UserStorageWrapper(checkNotNull(customStorageWrapper));
    }
    @Override
    public long getChangeNumber(String segmentName) {
        String wrapperResponse = _userStorageWrapper.get(PrefixAdapter.buildSegment(segmentName));
        return Helper.responseToLong(wrapperResponse, -1L);
    }

    @Override
    public void updateSegment(String segmentName, List<String> toAdd, List<String> toRemove, long changeNumber) {
        String keySegment = PrefixAdapter.buildSegment(segmentName);
        _userStorageWrapper.addItems(keySegment, toAdd);
        _userStorageWrapper.removeItems(keySegment, toRemove);
        _userStorageWrapper.set(PrefixAdapter.buildSegmentTill(segmentName), Json.toJson(changeNumber));
    }

    @Override
    public void setChangeNumber(String segmentName, long changeNumber) {
        _userStorageWrapper.set(PrefixAdapter.buildSegmentTill(segmentName), Json.toJson(changeNumber));
    }
}
