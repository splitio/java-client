package io.split.client.impressions;

import com.google.common.collect.Lists;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class UniqueKeysTrackerImp implements UniqueKeysTracker{
    private static final Logger _log = LoggerFactory.getLogger(UniqueKeysTrackerImp.class);
    private static final double MARGIN_ERROR = 0.01;
    private static final int MAX_UNIQUE_KEYS_POST_SIZE = 5000;
    private static final int MAX_AMOUNT_OF_KEYS = 10000000;
    private final AtomicInteger trackerKeysSize = new AtomicInteger(0);
    private FilterAdapter filterAdapter;
    private final TelemetrySynchronizer _telemetrySynchronizer;
    private final ScheduledExecutorService _uniqueKeysSyncScheduledExecutorService;
    private final ScheduledExecutorService _cleanFilterScheduledExecutorService;
    private final ConcurrentHashMap<String,HashSet<String>> uniqueKeysTracker;
    private final int _uniqueKeysRefreshRate;
    private final int _filterRefreshRate;
    private final AtomicBoolean sendGuard = new AtomicBoolean(false);
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
        uniqueKeysTracker.compute(featureFlagName,
                (feature, current) -> {
                    HashSet<String> keysByFeature = Optional.ofNullable(current).orElse(new HashSet<>());
                    keysByFeature.add(key);
                    trackerKeysSize.incrementAndGet();
                    return keysByFeature;
                });
        _logger.debug("The feature flag " + featureFlagName + " and key " + key + " was added");
        if (trackerKeysSize.intValue() >= MAX_UNIQUE_KEYS_POST_SIZE){
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
        trackerKeysSize.set(0);
        return toReturn;
    }

    private void sendUniqueKeys(){
        if (!sendGuard.compareAndSet(false, true)) {
            _log.debug("SendUniqueKeys already running");
            return;
        }

        try {
            if (uniqueKeysTracker.isEmpty()) {
                _log.debug("The Unique Keys Tracker is empty");
                return;
            }

            HashMap<String, HashSet<String>> uniqueKeysHashMap = popAll();
            List<UniqueKeys.UniqueKey> uniqueKeysFromPopAll = new ArrayList<>();
            for (Map.Entry<String, HashSet<String>> uniqueKeyEntry : uniqueKeysHashMap.entrySet()) {
                UniqueKeys.UniqueKey uniqueKey = new UniqueKeys.UniqueKey(uniqueKeyEntry.getKey(), new ArrayList<>(uniqueKeyEntry.getValue()));
                uniqueKeysFromPopAll.add(uniqueKey);
            }
            uniqueKeysFromPopAll = capChunksToMaxSize(uniqueKeysFromPopAll);

            for (List<UniqueKeys.UniqueKey> chunk : getChunks(uniqueKeysFromPopAll)) {
                _telemetrySynchronizer.synchronizeUniqueKeys(new UniqueKeys(chunk));
            }
        } finally {
            sendGuard.set(false);
        }
    }

    private List<UniqueKeys.UniqueKey> capChunksToMaxSize(List<UniqueKeys.UniqueKey> uniqueKeys) {
        List<UniqueKeys.UniqueKey> finalChunk = new ArrayList<>();
        for (UniqueKeys.UniqueKey uniqueKey : uniqueKeys) {
            if (uniqueKey.keysDto.size() > MAX_UNIQUE_KEYS_POST_SIZE) {
                for(List<String> subChunk : Lists.partition(uniqueKey.keysDto, MAX_UNIQUE_KEYS_POST_SIZE)) {
                    finalChunk.add(new UniqueKeys.UniqueKey(uniqueKey.featureName, subChunk));
                }
                continue;
            }
            finalChunk.add(uniqueKey);
        }
        return finalChunk;
    }

    private List<List<UniqueKeys.UniqueKey>> getChunks(List<UniqueKeys.UniqueKey> uniqueKeys) {
        List<List<UniqueKeys.UniqueKey>> chunks = new ArrayList<>();
        List<UniqueKeys.UniqueKey> intermediateChunk = new ArrayList<>();
        for (UniqueKeys.UniqueKey uniqueKey : uniqeKeys) {
            if ((getChunkSize(intermediateChunk) + uniqueKey.keysDto.size()) > MAX_UNIQUE_KEYS_POST_SIZE) {
                chunks.add(intermediateChunk);
                intermediateChunk = new ArrayList<>();
            }
            intermediateChunk.add(uniqueKey);
        }
        if (!intermediateChunk.isEmpty()) {
            chunks.add(intermediateChunk);
        }
        return chunks;
    }

    private int getChunkSize(List<UniqueKeys.UniqueKey> uniqueKeysChunk) {
        int totalSize = 0;
        for (UniqueKeys.UniqueKey uniqueKey : uniqueKeysChunk) {
            totalSize += uniqueKey.keysDto.size();
        }
        return totalSize;
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

    public AtomicBoolean getSendGuard() {
        return sendGuard;
    }
}
