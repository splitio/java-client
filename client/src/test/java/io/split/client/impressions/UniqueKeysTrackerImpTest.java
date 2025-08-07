package io.split.client.impressions;

import io.split.client.dtos.UniqueKeys;
import io.split.telemetry.synchronizer.TelemetryInMemorySubmitter;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

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
        for (Integer i=1; i<6000; i++) {
            if (i <= 1000) {
                Assert.assertTrue(uniqueKeysTrackerImp.track("feature1", "key" + i.toString()));
            }
            if (i <= 2000) {
                Assert.assertTrue(uniqueKeysTrackerImp.track("feature2", "key" + i.toString()));
            }
            if (i <= 3000) {
                Assert.assertTrue(uniqueKeysTrackerImp.track("feature3", "key" + i.toString()));
            }
            if (i <= 4000) {
                Assert.assertTrue(uniqueKeysTrackerImp.track("feature4", "key" + i.toString()));
            }
            Assert.assertTrue(uniqueKeysTrackerImp.track("feature5", "key" + i.toString()));
        }

        Method methodTrackerSize = uniqueKeysTrackerImp.getClass().getDeclaredMethod("getTrackerKeysSize");
        methodTrackerSize.setAccessible(true);
        int totalSize = (int) methodTrackerSize.invoke(uniqueKeysTrackerImp);
        Assert.assertTrue(totalSize == 15999);

        HashMap<String, HashSet<String>> uniqueKeysHashMap = uniqueKeysTrackerImp.popAll();
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
}