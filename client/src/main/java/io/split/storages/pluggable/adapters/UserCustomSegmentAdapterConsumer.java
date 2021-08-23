package io.split.storages.pluggable.adapters;

import io.split.storages.SegmentCacheConsumer;
import io.split.storages.pluggable.CustomStorageWrapper;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.SafeUserStorageWrapper;
import io.split.storages.pluggable.utils.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCustomSegmentAdapterConsumer implements SegmentCacheConsumer {

    private static final Logger _log = LoggerFactory.getLogger(UserCustomSegmentAdapterConsumer.class);

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
        return 0;
    }

    @Override
    public long getKeyCount() {
        Set<String> keys = _safeUserStorageWrapper.getKeysByPrefix(PrefixAdapter.buildSegment("fake"));
        long keysCount = 0L;
        if(keys == null) {
            return keysCount;
        }
        keys.stream()
                .map(key -> _safeUserStorageWrapper.getItemsCount(key));
        keysCount = _safeUserStorageWrapper.getItemsCount("");
        return 0;
    }
}
