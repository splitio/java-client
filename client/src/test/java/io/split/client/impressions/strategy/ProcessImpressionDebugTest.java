package io.split.client.impressions.strategy;

import static io.split.client.impressions.ImpressionTestUtils.keyImpression;

import io.split.client.dtos.KeyImpression;
import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionObserver;
import io.split.client.impressions.ImpressionsResult;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

public class ProcessImpressionDebugTest {

    private static final long LAST_SEEN_CACHE_SIZE = 500000;
    private static TelemetryStorage TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);

    @Test
    public void processImpressionsWithListener(){
        boolean listenerEnable = true;
        ImpressionObserver impressionObserver = new ImpressionObserver(LAST_SEEN_CACHE_SIZE);
        ProcessImpressionDebug processImpressionDebug = new ProcessImpressionDebug(listenerEnable, impressionObserver);

        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, null, null);
        KeyImpression ki2 = keyImpression("test2", "adil", "on", 1L, null, null);
        KeyImpression ki3 = keyImpression("test1", "adil", "on", 1L, null, null);

        List<Impression> impressions = new ArrayList<>();
        impressions.add(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null, null));
        impressions.add(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null, null));
        impressions.add(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null, null));

        ImpressionsResult impressionsResult1 = processImpressionDebug.process(impressions);

        long pt3 = impressionsResult1.getImpressionsToQueue().get(2).pt();
        Assert.assertEquals(1, pt3);

        Assert.assertEquals(3,impressionsResult1.getImpressionsToQueue().size());
        Assert.assertEquals(3,impressionsResult1.getImpressionsToListener().size());
    }

    @Test
    public void processImpressionsWithoutListener(){
        boolean listenerEnable = false;
        ImpressionObserver impressionObserver = new ImpressionObserver(LAST_SEEN_CACHE_SIZE);
        ProcessImpressionDebug processImpressionDebug = new ProcessImpressionDebug(listenerEnable, impressionObserver);

        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, null, null);
        KeyImpression ki2 = keyImpression("test2", "adil", "on", 1L, null, null);
        KeyImpression ki3 = keyImpression("test1", "adil", "on", 1L, null, null);

        List<Impression> impressions = new ArrayList<>();
        impressions.add(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null, null));
        impressions.add(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null, null));
        impressions.add(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null, null));

        ImpressionsResult impressionsResult1 = processImpressionDebug.process(impressions);

        long pt3 = impressionsResult1.getImpressionsToQueue().get(2).pt();
        Assert.assertEquals(1, pt3);

        Assert.assertEquals(3,impressionsResult1.getImpressionsToQueue().size());
        Assert.assertNull(impressionsResult1.getImpressionsToListener());
    }
}