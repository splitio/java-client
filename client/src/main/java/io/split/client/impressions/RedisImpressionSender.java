package io.split.client.impressions;

import io.split.client.dtos.TestImpressions;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.UserPipelineWrapper;
import io.split.storages.pluggable.domain.userStorageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pluggable.CustomStorageWrapper;

import java.util.HashMap;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class RedisImpressionSender implements ImpressionsSender{

    private final userStorageWrapper _userStorageWrapper;

    private static final Logger _logger = LoggerFactory.getLogger(RedisImpressionSender.class);

    public static RedisImpressionSender create(CustomStorageWrapper customStorageWrapper){
        return new RedisImpressionSender(customStorageWrapper);
    }

    private RedisImpressionSender(CustomStorageWrapper customStorageWrapper) {
        this._userStorageWrapper = new userStorageWrapper(checkNotNull(customStorageWrapper));
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
                pipelineExecution.hIncrement(key, countsKey.featureName() + "::" + countsKey.timeFrame(), counts.get(countsKey));
            }
            pipelineExecution.exec();
        } catch (Exception e){
            _logger.warn("Redis pipeline exception when posting counters: ", e);
        }
    }
}