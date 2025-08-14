package io.split.client.impressions;

import io.split.client.dtos.UniqueKeys;
import io.split.telemetry.synchronizer.TelemetryInMemorySubmitter;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class UniqueKeysTrackerImpTest {
    private static TelemetrySynchronizer _telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);

    @Test
    public void addSomeElements(){
        UniqueKeysTrackerImp uniqueKeysTrackerImp = new UniqueKeysTrackerImp(_telemetrySynchronizer, 10000, 10000, null);
        Assert.assertTrue(uniqueKeysTrackerImp.track("feature1","key1"));
        Assert.assertTrue(uniqueKeysTrackerImp.track("feature1","key2"));
        Assert.assertTrue(uniqueKeysTrackerImp.track("feature1","key3"));
        Assert.assertTrue(uniqueKeysTrackerImp.track("feature2","key4"));
        Assert.assertTrue(uniqueKeysTrackerImp.track("feature2","key5"));

        HashMap<String, HashSet<String>> result = uniqueKeysTrackerImp.popAll();
        Assert.assertEquals(2,result.size());

        HashSet<String> value1 = result.get("feature1");
        Assert.assertEquals(3,value1.size());
        Assert.assertTrue(value1.contains("key1"));
        Assert.assertTrue(value1.contains("key2"));
        Assert.assertTrue(value1.contains("key3"));

        HashSet<String> value2 = result.get("feature2");
        Assert.assertEquals(2,value2.size());
        Assert.assertTrue(value2.contains("key4"));
        Assert.assertTrue(value2.contains("key5"));
    }

    @Test
    public void addTheSameElements(){
        UniqueKeysTrackerImp uniqueKeysTrackerImp = new UniqueKeysTrackerImp(_telemetrySynchronizer, 10000, 10000, null);
        Assert.assertTrue(uniqueKeysTrackerImp.track("feature1","key1"));
        Assert.assertTrue(uniqueKeysTrackerImp.track("feature1","key2"));
        Assert.assertTrue(uniqueKeysTrackerImp.track("feature1","key3"));

        Assert.assertFalse(uniqueKeysTrackerImp.track("feature1","key1"));
        Assert.assertFalse(uniqueKeysTrackerImp.track("feature1","key2"));
        Assert.assertFalse(uniqueKeysTrackerImp.track("feature1","key3"));

        HashMap<String, HashSet<String>> result = uniqueKeysTrackerImp.popAll();
        Assert.assertEquals(1,result.size());

        HashSet<String> value1 = result.get("feature1");
        Assert.assertEquals(3,value1.size());
        Assert.assertTrue(value1.contains("key1"));
        Assert.assertTrue(value1.contains("key2"));
        Assert.assertTrue(value1.contains("key3"));
    }

    @Test
    public void popAllUniqueKeys(){
        UniqueKeysTrackerImp uniqueKeysTrackerImp = new UniqueKeysTrackerImp(_telemetrySynchronizer, 10000, 10000, null);
        Assert.assertTrue(uniqueKeysTrackerImp.track("feature1","key1"));
        Assert.assertTrue(uniqueKeysTrackerImp.track("feature1","key2"));
        Assert.assertTrue(uniqueKeysTrackerImp.track("feature2","key3"));

        HashMap<String, HashSet<String>> result = uniqueKeysTrackerImp.popAll();
        Assert.assertEquals(2,result.size());
        HashMap<String, HashSet<String>> resultAfterPopAll = uniqueKeysTrackerImp.popAll();
        Assert.assertEquals(0,resultAfterPopAll.size());
    }

    @Test
    public void testSynchronization() throws Exception {
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);
        UniqueKeysTrackerImp uniqueKeysTrackerImp = new UniqueKeysTrackerImp(telemetrySynchronizer, 1, 3, null);
        uniqueKeysTrackerImp.start();
        Assert.assertTrue(uniqueKeysTrackerImp.track("feature1","key1"));
        Assert.assertTrue(uniqueKeysTrackerImp.track("feature1","key2"));
        Assert.assertTrue(uniqueKeysTrackerImp.track("feature2","key3"));

        Thread.sleep(2900);
        Mockito.verify(telemetrySynchronizer, Mockito.times(1)).synchronizeUniqueKeys(Mockito.anyObject());
        Thread.sleep(2900);
        Mockito.verify(telemetrySynchronizer, Mockito.times(1)).synchronizeUniqueKeys(Mockito.anyObject());
    }

    @Test
    public void testStopSynchronization() throws Exception {
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);
        UniqueKeysTrackerImp uniqueKeysTrackerImp = new UniqueKeysTrackerImp(telemetrySynchronizer, 1, 2, null);
        uniqueKeysTrackerImp.start();
        Assert.assertFalse(uniqueKeysTrackerImp.getSendGuard().get());
        Assert.assertTrue(uniqueKeysTrackerImp.track("feature1","key1"));
        Assert.assertTrue(uniqueKeysTrackerImp.track("feature1","key2"));
        Assert.assertTrue(uniqueKeysTrackerImp.track("feature2","key3"));

        Thread.sleep(2100);
        Mockito.verify(telemetrySynchronizer, Mockito.times(1)).synchronizeUniqueKeys(Mockito.anyObject());
        uniqueKeysTrackerImp.stop();
        Mockito.verify(telemetrySynchronizer, Mockito.times(1)).synchronizeUniqueKeys(Mockito.anyObject());
    }

    @Test
    public void testUniqueKeysChunks() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        UniqueKeysTrackerImp uniqueKeysTrackerImp = new UniqueKeysTrackerImp(_telemetrySynchronizer, 10000, 10000, null);
        HashMap<String, HashSet<String>> uniqueKeysHashMap = new HashMap<>();
        HashSet<String> feature1 = new HashSet<>();
        HashSet<String> feature2 = new HashSet<>();
        HashSet<String> feature3 = new HashSet<>();
        HashSet<String> feature4 = new HashSet<>();
        HashSet<String> feature5 = new HashSet<>();
        for (Integer i=1; i<6000; i++) {
            if (i <= 1000) {
                feature1.add("key" + i);
            }
            if (i <= 2000) {
                feature2.add("key" + i);
            }
            if (i <= 3000) {
                feature3.add("key" + i);
            }
            if (i <= 4000) {
                feature4.add("key" + i);
            }
            feature5.add("key" + i);
        }
        uniqueKeysHashMap.put("feature1", feature1);
        uniqueKeysHashMap.put("feature2", feature2);
        uniqueKeysHashMap.put("feature3", feature3);
        uniqueKeysHashMap.put("feature4", feature4);
        uniqueKeysHashMap.put("feature5", feature5);

        List<UniqueKeys.UniqueKey> uniqueKeysFromPopAll = new ArrayList<>();
        for (Map.Entry<String, HashSet<String>> uniqueKeyEntry : uniqueKeysHashMap.entrySet()) {
            UniqueKeys.UniqueKey uniqueKey = new UniqueKeys.UniqueKey(uniqueKeyEntry.getKey(), new ArrayList<>(uniqueKeyEntry.getValue()));
            uniqueKeysFromPopAll.add(uniqueKey);
        }
        Method methodCapChunks = uniqueKeysTrackerImp.getClass().getDeclaredMethod("capChunksToMaxSize", List.class);
        methodCapChunks.setAccessible(true);
        uniqueKeysFromPopAll = (List<UniqueKeys.UniqueKey>)methodCapChunks.invoke(uniqueKeysTrackerImp, uniqueKeysFromPopAll);

        Method methodGetChunks = uniqueKeysTrackerImp.getClass().getDeclaredMethod("getChunks", List.class);
        methodGetChunks.setAccessible(true);
        List<List<UniqueKeys.UniqueKey>> keysChunks = (List<List<UniqueKeys.UniqueKey>>) methodGetChunks.invoke(uniqueKeysTrackerImp, uniqueKeysFromPopAll);
        for (List<UniqueKeys.UniqueKey> chunk : keysChunks) {
            int chunkSize = 0;
            for (UniqueKeys.UniqueKey keys : chunk) {
                chunkSize += keys.keysDto.size();
            }
            Assert.assertTrue(chunkSize <= 5000);
        }
    }

    @Test
    public void testTrackReachMaxKeys() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);
        UniqueKeysTrackerImp uniqueKeysTrackerImp = new UniqueKeysTrackerImp(telemetrySynchronizer, 10000, 10000, null);
        for (int i=1; i<6000; i++) {
            Assert.assertTrue(uniqueKeysTrackerImp.track("feature1", "key" + i));
            Assert.assertTrue(uniqueKeysTrackerImp.track("feature2", "key" + i));
        }
        Mockito.verify(telemetrySynchronizer, Mockito.times(2)).synchronizeUniqueKeys(Mockito.anyObject());

        Field getTrackerSize = uniqueKeysTrackerImp.getClass().getDeclaredField("trackerKeysSize");
        getTrackerSize.setAccessible(true);
        AtomicInteger trackerSize = (AtomicInteger) getTrackerSize.get(uniqueKeysTrackerImp);
        Assert.assertTrue(trackerSize.intValue() == 1998);
    }
}