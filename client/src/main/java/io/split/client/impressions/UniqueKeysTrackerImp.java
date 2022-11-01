package io.split.client.impressions;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.client.dtos.UniqueKeys;
import io.split.client.impressions.filters.BloomFilterImp;
import io.split.client.impressions.filters.Filter;
import io.split.client.impressions.filters.FilterAdapter;
import io.split.client.impressions.filters.FilterAdapterImpl;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
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

    public UniqueKeysTrackerImp(TelemetrySynchronizer telemetrySynchronizer, int uniqueKeysRefreshRate, int filterRefreshRate) {
        Filter bloomFilter = new BloomFilterImp(MAX_AMOUNT_OF_KEYS, MARGIN_ERROR);
        this.filterAdapter = new FilterAdapterImpl(bloomFilter);
        uniqueKeysTracker = new ConcurrentHashMap<>();
        _telemetrySynchronizer = telemetrySynchronizer;
        _uniqueKeysRefreshRate = uniqueKeysRefreshRate;
        _filterRefreshRate = filterRefreshRate;

        ThreadFactory uniqueKeysSyncThreadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("UniqueKeys-sync-%d")
                .build();
        ThreadFactory filterThreadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Filter-%d")
                .build();
        _uniqueKeysSyncScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(uniqueKeysSyncThreadFactory);
        _cleanFilterScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(filterThreadFactory);
    }

    @Override
    public synchronized boolean track(String featureName, String key) {
        if (!filterAdapter.add(featureName, key)) {
            _logger.debug("The feature " + featureName + " and key " + key + " exist in the UniqueKeysTracker");
            return false;
        }
        HashSet<String> value = new HashSet<>();
        if(uniqueKeysTracker.containsKey(featureName)){
            value = uniqueKeysTracker.get(featureName);
        }
        value.add(key);
        uniqueKeysTracker.put(featureName, value);
        _logger.debug("The feature " + featureName + " and key " + key + " was added");
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

    private void scheduleWithFixedDelay(ScheduledExecutorService scheduledExecutorService, int refreshRate, ExecuteUniqueKeysAction executeUniqueKeysAction) {
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

    private void sendUniqueKeys(){
        if (uniqueKeysTracker.size() == 0) {
           _log.warn("The Unique Keys Tracker is empty");
           return;
        }
        HashMap<String, HashSet<String>> uniqueKeysHashMap = popAll();
        List<UniqueKeys.UniqueKey> uniqueKeysFromPopAll = new ArrayList<>();
        for (String feature : uniqueKeysHashMap.keySet()) {
            UniqueKeys.UniqueKey uniqueKey = new UniqueKeys.UniqueKey(feature, new ArrayList<>(uniqueKeysHashMap.get(feature)));
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
