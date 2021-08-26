package io.split.storages.pluggable.adapters;

import io.split.storages.SegmentCacheConsumer;
import io.split.storages.pluggable.CustomStorageWrapper;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.SafeUserStorageWrapper;
import io.split.storages.pluggable.utils.Helper;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCustomSegmentAdapterConsumer implements SegmentCacheConsumer {

    private final SafeUserStorageWrapper _safeUserStorageWrapper;

    public UserCustomSegmentAdapterConsumer(CustomStorageWrapper customStorageWrapper) {
        _safeUserStorageWrapper = new SafeUserStorageWrapper(checkNotNull(customStorageWrapper));
    }

    @Override
    public long getChangeNumber(String segmentName) {
        String wrapperResponse = _safeUserStorageWrapper.get(PrefixAdapter.buildSegment(segmentName));
        return Helper.responseToLong(wrapperResponse, -1L);
    }

    @Override
    public boolean isInSegment(String segmentName, String key) {
        return _safeUserStorageWrapper.itemContains(PrefixAdapter.buildSegment(segmentName), key);
    }

    @Override
    public long getSegmentCount() {
        Set<String> keys = _safeUserStorageWrapper.getKeysByPrefix(PrefixAdapter.buildSegmentAll());
        return keys == null ? 0L : keys.size();
    }

    @Override
    public long getKeyCount() {
        Set<String> keys = _safeUserStorageWrapper.getKeysByPrefix(PrefixAdapter.buildSegmentAll());
        long keysCount = 0L;
        if(keys == null) {
            return keysCount;
        }
        return keys.stream().mapToLong(key -> _safeUserStorageWrapper.getItemsCount(key)).sum();
    }
}
