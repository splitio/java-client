package io.split.client.impressions;

import com.google.common.annotations.VisibleForTesting;
import io.split.client.impressions.filters.BloomFilterImp;
import io.split.client.impressions.filters.Filter;
import io.split.client.impressions.filters.FilterAdapter;
import io.split.client.impressions.filters.FilterAdapterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class UniqueKeysTrackerImp implements UniqueKeysTracker{
    private static final double MARGIN_ERROR = 0.01;
    private static final int MAX_AMOUNT_OF_TRACKED_MTKS = 30000;
    private static final int MAX_AMOUNT_OF_KEYS = 10000000;
    private FilterAdapter filterAdapter;
    private ImpressionsSender impressionsSender;
    private final ConcurrentHashMap<String,HashSet<String>> mtkTracker;
    private static final Logger _logger = LoggerFactory.getLogger(UniqueKeysTrackerImp.class);

    public UniqueKeysTrackerImp() {
        Filter bloomFilter = new BloomFilterImp(MAX_AMOUNT_OF_KEYS, MARGIN_ERROR);
        this.filterAdapter = new FilterAdapterImpl(bloomFilter);
        mtkTracker = new ConcurrentHashMap<>();
    }

    @Override
    public boolean track(String featureName, String key) {
        if (mtkTracker.size() == MAX_AMOUNT_OF_TRACKED_MTKS){
            //@todo implements add logic to flush data when the Dictionary is complete.
            //flush
            _logger.warn("The MTKTracker size reached the maximum limit");
            return false;
        }
        if (filterAdapter.add(featureName, key)) {
            HashSet<String> value = new HashSet<>();
            if(mtkTracker.containsKey(featureName)){
                value = mtkTracker.get(featureName);
            }
            value.add(key);
            mtkTracker.put(featureName, value);
            _logger.debug("The feature " + featureName + " and key " + key + " was added");
            return true;
        }
        _logger.debug("The feature " + featureName + " and key " + key + " exist in the MtkTracker");
        return false;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
    @VisibleForTesting
    ConcurrentHashMap<String, HashSet<String>> getMtkTracker() {
        return mtkTracker;
    }
}
