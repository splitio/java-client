package io.split.client.impressions;

import io.split.client.dtos.UniqueKeys;
import io.split.client.impressions.filters.BloomFilterImp;
import io.split.client.impressions.filters.Filter;
import io.split.client.impressions.filters.FilterAdapter;
import io.split.client.impressions.filters.FilterAdapterImpl;
import io.split.client.utils.SplitExecutorFactory;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class UniqueKeysTrackerImp implements UniqueKeysTracker{
    private static final Logger _log = LoggerFactory.getLogger(UniqueKeysTrackerImp.class);
    private static final double MARGIN_ERROR = 0.01;
    private static final int MAX_AMOUNT_OF_TRACKED_UNIQUE_KEYS = 30000;
    private static final int MAX_AMOUNT_OF_KEYS = 10000000;
    private FilterAdapter filterAdapter;
    private final TelemetrySynchronizer _telemetrySynchronizer;
    private final ScheduledExecutorService _uniqueKeysSyncScheduledExecutorService;
    private final ScheduledExecutorService _cleanFilterScheduledExecutorService;
    private final ConcurrentHashMap<String,HashSet<String>> uniqueKeysTracker;
    private final int _uniqueKeysRefreshRate;
    private final int _filterRefreshRate;
    private static final Logger _logger = LoggerFactory.getLogger(UniqueKeysTrackerImp.class);

    public UniqueKeysTrackerImp(TelemetrySynchronizer telemetrySynchronizer, int uniqueKeysRefreshRate, int filterRefreshRate,
                                ThreadFactory threadFactory) {
        Filter bloomFilter = new BloomFilterImp(MAX_AMOUNT_OF_KEYS, MARGIN_ERROR);
        this.filterAdapter = new FilterAdapterImpl(bloomFilter);
        uniqueKeysTracker = new ConcurrentHashMap<>();
        _telemetrySynchronizer = telemetrySynchronizer;
        _uniqueKeysRefreshRate = uniqueKeysRefreshRate;
        _filterRefreshRate = filterRefreshRate;
        _uniqueKeysSyncScheduledExecutorService = SplitExecutorFactory.buildSingleThreadScheduledExecutor(threadFactory,"UniqueKeys-sync-%d");
        _cleanFilterScheduledExecutorService = SplitExecutorFactory.buildSingleThreadScheduledExecutor(threadFactory,"Filter-%d");
    }

    @Override
    public boolean track(String featureFlagName, String key) {
        if (!filterAdapter.add(featureFlagName, key)) {
            _logger.debug("The feature flag " + featureFlagName + " and key " + key + " exist in the UniqueKeysTracker");
            return false;
        }
        HashSet<String> value = new HashSet<>();
        if(uniqueKeysTracker.containsKey(featureFlagName)){
            value = uniqueKeysTracker.get(featureFlagName);
        }
        value.add(key);
        uniqueKeysTracker.put(featureFlagName, value);
        _logger.debug("The feature flag " + featureFlagName + " and key " + key + " was added");
        if (uniqueKeysTracker.size() == MAX_AMOUNT_OF_TRACKED_UNIQUE_KEYS){
            _logger.warn("The UniqueKeysTracker size reached the maximum limit");
            try {
                sendUniqueKeys();
            } catch (Exception e) {
                _log.error("Error sending unique keys.", e);
            }
        }
        return true;
    }

    @Override
    public void start() {
        scheduleWithFixedDelay(_uniqueKeysSyncScheduledExecutorService, _uniqueKeysRefreshRate, new ExecuteSendUniqueKeys());
        scheduleWithFixedDelay(_cleanFilterScheduledExecutorService, _filterRefreshRate, new ExecuteCleanFilter());
    }

    private void scheduleWithFixedDelay(ScheduledExecutorService scheduledExecutorService, int refreshRate,
                                        ExecuteUniqueKeysAction executeUniqueKeysAction) {
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                executeUniqueKeysAction.execute();
            } catch (Exception e) {
                _log.error("Error executing an Unique Key Action.", e);
            }
        }, refreshRate, refreshRate, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        try {
            sendUniqueKeys();
        } catch (Exception e) {
            _log.error("Error sending unique keys.");
        }
        _uniqueKeysSyncScheduledExecutorService.shutdown();
        _cleanFilterScheduledExecutorService.shutdown();
    }

    public HashMap<String,HashSet<String>> popAll(){
        HashMap<String,HashSet<String>> toReturn = new HashMap<>();
        for (String key : uniqueKeysTracker.keySet()) {
            HashSet<String> value = uniqueKeysTracker.remove(key);
            toReturn.put(key, value);
        }
        return toReturn;
    }

    private synchronized void sendUniqueKeys(){
        if (uniqueKeysTracker.size() == 0) {
           _log.warn("The Unique Keys Tracker is empty");
           return;
        }
        HashMap<String, HashSet<String>> uniqueKeysHashMap = popAll();
        List<UniqueKeys.UniqueKey> uniqueKeysFromPopAll = new ArrayList<>();
        for (String featureFlag : uniqueKeysHashMap.keySet()) {
            UniqueKeys.UniqueKey uniqueKey = new UniqueKeys.UniqueKey(featureFlag, new ArrayList<>(uniqueKeysHashMap.get(featureFlag)));
            uniqueKeysFromPopAll.add(uniqueKey);
        }
        _telemetrySynchronizer.synchronizeUniqueKeys(new UniqueKeys(uniqueKeysFromPopAll));
    }

    private interface ExecuteUniqueKeysAction{
        void execute();
    }
    private class ExecuteCleanFilter implements ExecuteUniqueKeysAction {

        @Override
        public void execute() {
            filterAdapter.clear();
        }
    }

    private class ExecuteSendUniqueKeys implements ExecuteUniqueKeysAction {

        @Override
        public void execute() {
            sendUniqueKeys();
        }
    }
}
