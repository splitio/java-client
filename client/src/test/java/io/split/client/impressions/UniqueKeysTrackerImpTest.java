package io.split.client.impressions;

import io.split.telemetry.synchronizer.TelemetryInMemorySubmitter;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.HashSet;

public class UniqueKeysTrackerImpTest {

    @Test
    public void addSomeElements(){
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);
        UniqueKeysTrackerImp uniqueKeysTrackerImp = new UniqueKeysTrackerImp(telemetrySynchronizer, 10000, 10000);
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
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);
        UniqueKeysTrackerImp uniqueKeysTrackerImp = new UniqueKeysTrackerImp(telemetrySynchronizer, 10000, 10000);
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
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);
        UniqueKeysTrackerImp uniqueKeysTrackerImp = new UniqueKeysTrackerImp(telemetrySynchronizer, 10000, 10000);
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
        UniqueKeysTrackerImp uniqueKeysTrackerImp = new UniqueKeysTrackerImp(telemetrySynchronizer, 1, 3);
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
        UniqueKeysTrackerImp uniqueKeysTrackerImp = new UniqueKeysTrackerImp(telemetrySynchronizer, 1, 2);
        Assert.assertTrue(uniqueKeysTrackerImp.track("feature1","key1"));
        Assert.assertTrue(uniqueKeysTrackerImp.track("feature1","key2"));
        Assert.assertTrue(uniqueKeysTrackerImp.track("feature2","key3"));

        Thread.sleep(2100);
        Mockito.verify(telemetrySynchronizer, Mockito.times(1)).synchronizeUniqueKeys(Mockito.anyObject());
        uniqueKeysTrackerImp.stop();
        Mockito.verify(telemetrySynchronizer, Mockito.times(1)).synchronizeUniqueKeys(Mockito.anyObject());
    }
}