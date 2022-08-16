package io.split.storages.pluggable.adapters;

import io.split.storages.SegmentCacheConsumer;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.UserStorageWrapper;
import io.split.storages.pluggable.utils.Helper;
import pluggable.CustomStorageWrapper;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCustomSegmentAdapterConsumer implements SegmentCacheConsumer {

    private final UserStorageWrapper _userStorageWrapper;

    public UserCustomSegmentAdapterConsumer(CustomStorageWrapper customStorageWrapper) {
        _userStorageWrapper = new UserStorageWrapper(checkNotNull(customStorageWrapper));
    }

    @Override
    public long getChangeNumber(String segmentName) {
        String wrapperResponse = _userStorageWrapper.get(PrefixAdapter.buildSegment(segmentName));
        return Helper.responseToLong(wrapperResponse, -1L);
    }

    @Override
    public boolean isInSegment(String segmentName, String key) {
        return _userStorageWrapper.itemContains(PrefixAdapter.buildSegment(segmentName), key);
    }

    @Override
    public long getSegmentCount() {
        Set<String> keys = _userStorageWrapper.getKeysByPrefix(PrefixAdapter.buildSegmentAll());
        return keys == null ? 0L : keys.size();
    }

    @Override
    public long getKeyCount() {
        Set<String> keys = _userStorageWrapper.getKeysByPrefix(PrefixAdapter.buildSegmentAll());
        if(keys == null) {
            return 0L;
        }
        return keys.stream().mapToLong(key -> _userStorageWrapper.getItemsCount(key)).sum();
    }
}
