package io.split.client.impressions;

import io.split.client.dtos.TestImpressions;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.SafeUserStorageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pluggable.CustomStorageWrapper;
import pluggable.Pipeline;

import java.util.HashMap;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class RedisImpressionSender implements ImpressionsSender{

    private final SafeUserStorageWrapper _safeUserStorageWrapper;
    private static final Logger _logger = LoggerFactory.getLogger(RedisImpressionSender.class);

    public static RedisImpressionSender create(CustomStorageWrapper customStorageWrapper){
        return new RedisImpressionSender(customStorageWrapper);
    }

    private RedisImpressionSender(CustomStorageWrapper customStorageWrapper) {
        this._safeUserStorageWrapper = new SafeUserStorageWrapper(checkNotNull(customStorageWrapper));
    }

    @Override
    public void postImpressionsBulk(List<TestImpressions> impressions) {
        //No-Op
    }

    @Override
    public void postCounters(HashMap<ImpressionCounter.Key, Integer> counts) {
        try {
            Pipeline pipelineExecution = _safeUserStorageWrapper.pipeline();
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
