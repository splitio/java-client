package io.split.storages.pluggable.adapters;

import io.split.engine.experiments.ParsedRuleBasedSegment;
import io.split.storages.RuleBasedSegmentCacheProducer;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.UserStorageWrapper;
import io.split.storages.pluggable.utils.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pluggable.CustomStorageWrapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCustomRuleBasedSegmentAdapterProducer implements RuleBasedSegmentCacheProducer {

    private static final Logger _log = LoggerFactory.getLogger(UserCustomRuleBasedSegmentAdapterProducer.class);

    private final UserStorageWrapper _userStorageWrapper;

    public UserCustomRuleBasedSegmentAdapterProducer(CustomStorageWrapper customStorageWrapper) {
        _userStorageWrapper = new UserStorageWrapper(checkNotNull(customStorageWrapper));
    }

    @Override
    public long getChangeNumber() {
        String wrapperResponse = _userStorageWrapper.get(PrefixAdapter.buildRuleBasedSegmentChangeNumber());
        return Helper.responseToLong(wrapperResponse, -1L);
    }

    @Override
    public boolean remove(String ruleBasedSegmentName) {
        // NoOp
        return true;
    }

    @Override
    public void setChangeNumber(long changeNumber) {
        //NoOp
    }

    @Override
    public void clear() {
        //NoOp
    }

    @Override
    public void update(List<ParsedRuleBasedSegment> toAdd, List<String> toRemove, long changeNumber) {
        //NoOp
    }

    public Set<String> getSegments() {
        //NoOp
        return new HashSet<>();
    }
}
