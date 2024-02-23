package io.split.engine.sse.workers;

import io.split.client.interceptors.FlagSetsFilter;
import io.split.client.interceptors.FlagSetsFilterImpl;
import io.split.engine.common.Synchronizer;
import io.split.engine.experiments.SplitParser;
import io.split.engine.sse.dtos.FeatureFlagChangeNotification;
import io.split.engine.sse.dtos.GenericNotificationData;
import io.split.engine.sse.dtos.SplitKillNotification;
import io.split.storages.SplitCacheProducer;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

public class SplitsWorkerTest {

    private static final FlagSetsFilter FLAG_SETS_FILTER = new FlagSetsFilterImpl(new HashSet<>());

    @Test
    public void addToQueueWithoutElementsWShouldNotTriggerFetch() throws InterruptedException {
        Synchronizer splitFetcherMock = Mockito.mock(Synchronizer.class);
        SplitParser splitParser = new SplitParser();
        SplitCacheProducer splitCacheProducer = Mockito.mock(SplitCacheProducer.class);
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(InMemoryTelemetryStorage.class);

        FeatureFlagsWorker featureFlagsWorker = new FeatureFlagWorkerImp(splitFetcherMock, splitParser, splitCacheProducer, telemetryRuntimeProducer, FLAG_SETS_FILTER);
        featureFlagsWorker.start();

        Thread.sleep(500);
        Mockito.verify(splitFetcherMock, Mockito.never()).refreshSplits(Mockito.anyObject());
        featureFlagsWorker.stop();
    }

    @Test
    public void addToQueueWithElementsWShouldTriggerFetch() throws InterruptedException {
        Synchronizer syncMock = Mockito.mock(Synchronizer.class);
        SplitParser splitParser = new SplitParser();
        SplitCacheProducer splitCacheProducer = Mockito.mock(SplitCacheProducer.class);
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(InMemoryTelemetryStorage.class);

        FeatureFlagsWorker featureFlagsWorker = new FeatureFlagWorkerImp(syncMock, splitParser, splitCacheProducer, telemetryRuntimeProducer, FLAG_SETS_FILTER);
        featureFlagsWorker.start();

        ArgumentCaptor<Long> cnCaptor = ArgumentCaptor.forClass(Long.class);

        featureFlagsWorker.addToQueue(new FeatureFlagChangeNotification(GenericNotificationData.builder()
                .changeNumber(1585956698457L)
                .build()));
        featureFlagsWorker.addToQueue(new FeatureFlagChangeNotification(GenericNotificationData.builder()
                .changeNumber(1585956698467L)
                .build()));
        featureFlagsWorker.addToQueue(new FeatureFlagChangeNotification(GenericNotificationData.builder()
                .changeNumber(1585956698477L)
                .build()));
        featureFlagsWorker.addToQueue(new FeatureFlagChangeNotification(GenericNotificationData.builder()
                .changeNumber(1585956698476L)
                .build()));
        Thread.sleep(1000);

        Mockito.verify(syncMock, Mockito.times(4)).refreshSplits(cnCaptor.capture());
        List<Long> captured = cnCaptor.getAllValues();
        assertThat(captured, contains(1585956698457L, 1585956698467L, 1585956698477L, 1585956698476L));
        featureFlagsWorker.stop();
    }

    @Test
    public void killShouldTriggerFetch() {
        long changeNumber = 1585956698457L;
        String featureFlagName = "feature-flag-test";
        String defaultTreatment = "off";

        Synchronizer syncMock = Mockito.mock(Synchronizer.class);
        SplitParser splitParser = new SplitParser();
        SplitCacheProducer splitCacheProducer = Mockito.mock(SplitCacheProducer.class);
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(InMemoryTelemetryStorage.class);
        FeatureFlagsWorker featureFlagsWorker = new FeatureFlagWorkerImp(syncMock, splitParser, splitCacheProducer, telemetryRuntimeProducer, FLAG_SETS_FILTER) {
        };
        featureFlagsWorker.start();
        SplitKillNotification splitKillNotification = new SplitKillNotification(GenericNotificationData.builder()
                .changeNumber(changeNumber)
                .defaultTreatment(defaultTreatment)
                .featureFlagName(featureFlagName)
                .build());

        featureFlagsWorker.kill(splitKillNotification);
        Mockito.verify(syncMock, Mockito.times(1)).localKillSplit(splitKillNotification);
        featureFlagsWorker.stop();
    }

    @Test
    public void messagesNotProcessedWhenWorkerStopped() throws InterruptedException {
        Synchronizer syncMock = Mockito.mock(Synchronizer.class);
        SplitParser splitParser = new SplitParser();
        SplitCacheProducer splitCacheProducer = Mockito.mock(SplitCacheProducer.class);
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(InMemoryTelemetryStorage.class);
        FeatureFlagsWorker featureFlagsWorker = new FeatureFlagWorkerImp(syncMock, splitParser, splitCacheProducer, telemetryRuntimeProducer, FLAG_SETS_FILTER);
        featureFlagsWorker.start();
        featureFlagsWorker.addToQueue(new FeatureFlagChangeNotification(GenericNotificationData.builder()
                .changeNumber(1585956698457L)
                .build()));
        Thread.sleep(500);


        featureFlagsWorker.stop();
        Thread.sleep(500);

        featureFlagsWorker.addToQueue(new FeatureFlagChangeNotification(GenericNotificationData.builder()
                .changeNumber(1585956698467L)
                .build()));
        Mockito.verify(syncMock, Mockito.times(1)).refreshSplits(Mockito.anyObject()); // Previous one!

        Mockito.reset(syncMock);
        featureFlagsWorker.start();
        featureFlagsWorker.addToQueue(new FeatureFlagChangeNotification(GenericNotificationData.builder()
                .changeNumber(1585956698477L)
                .build()));
        Thread.sleep(500);
        Mockito.verify(syncMock, Mockito.times(1)).refreshSplits(Mockito.anyObject());
        featureFlagsWorker.stop();
    }
}