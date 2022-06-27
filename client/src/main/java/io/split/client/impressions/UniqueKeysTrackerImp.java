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
import java.util.concurrent.*;

public class UniqueKeysTrackerImp implements UniqueKeysTracker{
    private static final Logger _log = LoggerFactory.getLogger(UniqueKeysTrackerImp.class);
    private static final double MARGIN_ERROR = 0.01;
    private static final int MAX_AMOUNT_OF_TRACKED_UNIQUE_KEYS = 30000;
    private static final int MAX_AMOUNT_OF_KEYS = 10000000;
    private FilterAdapter filterAdapter;
    private final TelemetrySynchronizer _telemetrySynchronizer;
    private final ScheduledExecutorService _telemetrySyncScheduledExecutorService;
    private final ConcurrentHashMap<String,HashSet<String>> uniqueKeysTracker;
    private final int _uniqueKeysTimeToSend;
    private static final Logger _logger = LoggerFactory.getLogger(UniqueKeysTrackerImp.class);

    public UniqueKeysTrackerImp(TelemetrySynchronizer telemetrySynchronizer, int uniqueKeysTimeToSend) {
        Filter bloomFilter = new BloomFilterImp(MAX_AMOUNT_OF_KEYS, MARGIN_ERROR);
        this.filterAdapter = new FilterAdapterImpl(bloomFilter);
        uniqueKeysTracker = new ConcurrentHashMap<>();
        _telemetrySynchronizer = telemetrySynchronizer;
        _uniqueKeysTimeToSend = uniqueKeysTimeToSend;
        ThreadFactory telemetrySyncThreadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Telemetry-sync-%d")
                .build();
        _telemetrySyncScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(telemetrySyncThreadFactory);
        try {
            this.start();
        } catch (Exception e) {
            _log.error("Error trying to init Unique Keys Tracker synchronizer task.", e);
        }
    }

    @Override
    public boolean track(String featureName, String key) {
        if (uniqueKeysTracker.size() == MAX_AMOUNT_OF_TRACKED_UNIQUE_KEYS){
            _logger.warn("The UniqueKeysTracker size reached the maximum limit");
            try {
                sendUniqueKeys();
            } catch (Exception e) {
                _log.error("Error sending telemetry stats.", e);
            }
            return false;
        }
        if (filterAdapter.add(featureName, key)) {
            HashSet<String> value = new HashSet<>();
            if(uniqueKeysTracker.containsKey(featureName)){
                value = uniqueKeysTracker.get(featureName);
            }
            value.add(key);
            uniqueKeysTracker.put(featureName, value);
            _logger.debug("The feature " + featureName + " and key " + key + " was added");
            return true;
        }
        _logger.debug("The feature " + featureName + " and key " + key + " exist in the UniqueKeysTracker");
        return false;
    }

    @Override
    public void start() {
        _telemetrySyncScheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                sendUniqueKeys();
            } catch (Exception e) {
                _log.warn("Error sending telemetry stats.");
            }
        },_uniqueKeysTimeToSend,  _uniqueKeysTimeToSend, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        try {
            sendUniqueKeys();
        } catch (Exception e) {
            _log.warn("Error sending telemetry stats.");
        }
        _telemetrySyncScheduledExecutorService.shutdown();
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
        HashMap<String,HashSet<String>> uniqueKeysHashMap = popAll();
        List<UniqueKeys.UniqueKey> uniqueKeysFromPopAll = new ArrayList<>();
        for(String feature: uniqueKeysHashMap.keySet()){
            UniqueKeys.UniqueKey uniqueKey = new UniqueKeys.UniqueKey(feature, new ArrayList<>(uniqueKeysHashMap.get(feature)));
            uniqueKeysFromPopAll.add(uniqueKey);
        }
        _telemetrySynchronizer.synchronizeUniqueKeys(new UniqueKeys(uniqueKeysFromPopAll));
    }
}
