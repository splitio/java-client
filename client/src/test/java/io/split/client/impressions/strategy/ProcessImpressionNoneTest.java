package io.split.client.impressions.strategy;

import io.split.client.dtos.KeyImpression;
import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionsResult;
import io.split.client.impressions.UniqueKeysTrackerImp;
import io.split.client.impressions.ImpressionCounter;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import io.split.telemetry.synchronizer.TelemetryInMemorySubmitter;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static io.split.client.impressions.ImpressionTestUtils.keyImpression;

public class ProcessImpressionNoneTest {

    private static TelemetryStorage TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);

    @Test
    public void processImpressionsWithListener(){
        boolean listenerEnable = true;
        ImpressionCounter counter = new ImpressionCounter();
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);
        UniqueKeysTrackerImp uniqueKeysTracker = new UniqueKeysTrackerImp(telemetrySynchronizer, 10000, 10000);
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(listenerEnable, uniqueKeysTracker, counter);

        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, null);
        KeyImpression ki2 = keyImpression("test2", "adil", "on", 1L, null);
        KeyImpression ki3 = keyImpression("test1", "adil", "on", 1L, null);

        List<Impression> impressions = new ArrayList<>();
        impressions.add(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null));
        impressions.add(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null));
        impressions.add(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null));

        ImpressionsResult impressionsResult1 = processImpressionNone.process(impressions);
        Assert.assertEquals(0,impressionsResult1.getImpressionsToQueue().size());
        Assert.assertEquals(3,impressionsResult1.getImpressionsToListener().size());
        Assert.assertEquals(2, uniqueKeysTracker.popAll().size());

        HashMap<ImpressionCounter.Key, Integer> counters = counter.popAll();
        Assert.assertEquals(2, counters.size());
    }

    @Test
    public void processImpressionsWithoutListener(){
        boolean listenerEnable = false;
        ImpressionCounter counter = new ImpressionCounter();
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);
        UniqueKeysTrackerImp uniqueKeysTracker = new UniqueKeysTrackerImp(telemetrySynchronizer, 10000, 10000);
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(listenerEnable, uniqueKeysTracker, counter);

        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, null);
        KeyImpression ki2 = keyImpression("test2", "adil", "on", 1L, null);
        KeyImpression ki3 = keyImpression("test1", "adil", "on", 1L, null);

        List<Impression> impressions = new ArrayList<>();
        impressions.add(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null));
        impressions.add(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null));
        impressions.add(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null));

        ImpressionsResult impressionsResult1 = processImpressionNone.process(impressions);
        Assert.assertEquals(0,impressionsResult1.getImpressionsToQueue().size());
        Assert.assertNull(impressionsResult1.getImpressionsToListener());
        Assert.assertEquals(2, uniqueKeysTracker.popAll().size());
        Assert.assertEquals(2, counter.popAll().size());
    }
}