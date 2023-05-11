package io.split.client.impressions;

import io.split.client.dtos.TestImpressions;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.UserPipelineWrapper;
import io.split.storages.pluggable.domain.UserStorageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pluggable.CustomStorageWrapper;

import java.util.HashMap;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class PluggableImpressionSender implements ImpressionsSender{

    private final UserStorageWrapper _userStorageWrapper;

    private static final Logger _logger = LoggerFactory.getLogger(PluggableImpressionSender.class);

    public static PluggableImpressionSender create(CustomStorageWrapper customStorageWrapper){
        return new PluggableImpressionSender(customStorageWrapper);
    }

    private PluggableImpressionSender(CustomStorageWrapper customStorageWrapper) {
        this._userStorageWrapper = new UserStorageWrapper(checkNotNull(customStorageWrapper));
    }

    @Override
    public void postImpressionsBulk(List<TestImpressions> impressions) {
        //No-Op
    }

    @Override
    public void postCounters(HashMap<ImpressionCounter.Key, Integer> counts) {
        try {
            UserPipelineWrapper pipelineExecution = _userStorageWrapper.pipeline();
            for(ImpressionCounter.Key countsKey: counts.keySet()){
                String key = PrefixAdapter.buildImpressionsCount();
                pipelineExecution.hIncrement(key, countsKey.featureFlagName() + "::" + countsKey.timeFrame(), counts.get(countsKey));
            }
            pipelineExecution.exec();
        } catch (Exception e){
            _logger.warn("Redis pipeline exception when posting counters: ", e);
        }
    }
}