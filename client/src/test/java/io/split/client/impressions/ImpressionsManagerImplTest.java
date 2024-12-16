package io.split.client.impressions;

import io.split.client.SplitClientConfig;
import io.split.client.dtos.DecoratedImpression;
import io.split.client.dtos.KeyImpression;
import io.split.client.dtos.TestImpressions;

import io.split.client.dtos.UniqueKeys;
import io.split.client.impressions.strategy.ProcessImpressionDebug;
import io.split.client.impressions.strategy.ProcessImpressionNone;
import io.split.client.impressions.strategy.ProcessImpressionOptimized;
import io.split.client.impressions.strategy.ProcessImpressionStrategy;
import io.split.storages.enums.OperationMode;
import io.split.telemetry.domain.enums.ImpressionsDataTypeEnum;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import io.split.telemetry.storage.TelemetryStorageProducer;
import io.split.telemetry.synchronizer.TelemetryInMemorySubmitter;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import pluggable.CustomStorageWrapper;

import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.split.client.impressions.ImpressionTestUtils.keyImpression;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.junit.Assert.assertEquals;

/**
 * Created by patricioe on 6/20/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class ImpressionsManagerImplTest {
    private static TelemetryStorage TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);

    @Before
    public void setUp() {
        TELEMETRY_STORAGE = Mockito.mock(InMemoryTelemetryStorage.class);
    }

    @Captor
    private ArgumentCaptor<List<TestImpressions>> impressionsCaptor;

    @Captor
    private ArgumentCaptor<List<KeyImpression>> impressionKeyList;

    @Captor
    private ArgumentCaptor<UniqueKeys> uniqueKeysCaptor;

    @Captor
    private ArgumentCaptor<HashMap<ImpressionCounter.Key, Integer>> impressionCountCaptor;

    @Test
    public void works() throws URISyntaxException {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(4)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ImpressionCounter impressionCounter = Mockito.mock(ImpressionCounter.class);
        ImpressionObserver impressionObserver = new ImpressionObserver(200);

        ProcessImpressionStrategy processImpressionStrategy = new ProcessImpressionDebug(false, impressionObserver);
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(false, null, null);

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, processImpressionNone, processImpressionStrategy, impressionCounter, null);
        treatmentLog.start();

        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, null);
        KeyImpression ki2 = keyImpression("test1", "adil", "on", 2L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 2L);
        KeyImpression ki4 = keyImpression("test2", "pato", "on", 4L, 3L);

        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, ki1.changeNumber, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, ki2.changeNumber, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, ki3.changeNumber, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, ki4.changeNumber, null), true)).collect(Collectors.toList()));

        // Do what the scheduler would do.
        treatmentLog.sendImpressions();

        verify(senderMock).postImpressionsBulk(impressionsCaptor.capture());

        List<TestImpressions> captured = impressionsCaptor.getValue();

        Assert.assertEquals(2, captured.size());
    }

    @Test
    public void testImpressionListenerOptimize() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.OPTIMIZED)
                .build();
        ImpressionsStorage storage = Mockito.mock(InMemoryImpressionsStorage.class);

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ImpressionCounter impressionCounter = new ImpressionCounter();
        ImpressionObserver impressionObserver = new ImpressionObserver(200);
        TelemetryStorageProducer telemetryStorageProducer = new InMemoryTelemetryStorage();

        ProcessImpressionStrategy processImpressionStrategy = new ProcessImpressionOptimized(true, impressionObserver, impressionCounter, telemetryStorageProducer);
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(false, null, null);

        ImpressionListener impressionListener = Mockito.mock(AsynchronousImpressionListener.class);

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, processImpressionNone, processImpressionStrategy, impressionCounter, impressionListener);
        treatmentLog.start();

        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki2 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 2L);
        KeyImpression ki4 = keyImpression("test2", "pato", "on", 4L, 3L);

        List<DecoratedImpression> impressionList = new ArrayList<>();
        impressionList.add(new DecoratedImpression(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, ki1.changeNumber, null), true));
        impressionList.add(new DecoratedImpression(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, ki2.changeNumber, null), true));
        impressionList.add(new DecoratedImpression(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, ki3.changeNumber, null), true));
        impressionList.add(new DecoratedImpression(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, ki4.changeNumber, null), true));

        treatmentLog.track(impressionList);
        verify(impressionListener, times(4)).log(Mockito.anyObject());

        verify(storage).put(impressionKeyList.capture());

        List captured = impressionKeyList.getValue();

        Assert.assertEquals(3, captured.size());
    }

    @Test
    public void testImpressionListenerDebug() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(6)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .build();
        ImpressionsStorage storage = Mockito.mock(InMemoryImpressionsStorage.class);

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ImpressionCounter impressionCounter = Mockito.mock(ImpressionCounter.class);
        ImpressionObserver impressionObserver = new ImpressionObserver(200);

        ProcessImpressionStrategy processImpressionStrategy = new ProcessImpressionDebug(true, impressionObserver);
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(false, null, null);

        ImpressionListener impressionListener = Mockito.mock(AsynchronousImpressionListener.class);

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, processImpressionNone, processImpressionStrategy, impressionCounter, impressionListener);
        treatmentLog.start();

        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki2 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 2L);
        KeyImpression ki4 = keyImpression("test2", "pato", "on", 4L, 3L);

        List<DecoratedImpression> impressionList = new ArrayList<>();
        impressionList.add(new DecoratedImpression(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, ki1.changeNumber, null), true));
        impressionList.add(new DecoratedImpression(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, ki2.changeNumber, null), true));
        impressionList.add(new DecoratedImpression(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, ki3.changeNumber, null), true));
        impressionList.add(new DecoratedImpression(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, ki4.changeNumber, null), true));

        treatmentLog.track(impressionList);
        verify(impressionListener, times(4)).log(Mockito.anyObject());

        verify(storage).put(impressionKeyList.capture());

        List captured = impressionKeyList.getValue();

        Assert.assertEquals(4, captured.size());
    }

    @Test
    public void testImpressionListenerNone() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.NONE)
                .build();
        ImpressionsStorage storage = Mockito.mock(InMemoryImpressionsStorage.class);

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);
        ImpressionCounter impressionCounter = new ImpressionCounter();
        UniqueKeysTracker uniqueKeysTracker = new UniqueKeysTrackerImp(telemetrySynchronizer, 1000, 1000, null);
        uniqueKeysTracker.start();

        ProcessImpressionStrategy processImpressionStrategy = new ProcessImpressionNone(true, uniqueKeysTracker, impressionCounter);
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(false, null, null);

        ImpressionListener impressionListener = Mockito.mock(AsynchronousImpressionListener.class);

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, processImpressionNone, processImpressionStrategy, impressionCounter, impressionListener);
        treatmentLog.start();

        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki2 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 2L);
        KeyImpression ki4 = keyImpression("test2", "pato", "on", 4L, 3L);

        List<DecoratedImpression> impressionList = new ArrayList<>();
        impressionList.add(new DecoratedImpression(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, ki1.changeNumber, null), true));
        impressionList.add(new DecoratedImpression(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, ki2.changeNumber, null), true));
        impressionList.add(new DecoratedImpression(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, ki3.changeNumber, null), true));
        impressionList.add(new DecoratedImpression(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, ki4.changeNumber, null), true));

        treatmentLog.track(impressionList);
        verify(impressionListener, times(4)).log(Mockito.anyObject());

        verify(storage).put(impressionKeyList.capture());

        List captured = impressionKeyList.getValue();

        Assert.assertEquals(0, captured.size());
    }

    @Test
    public void worksButDropsImpressions() {

        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(3)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ImpressionCounter impressionCounter = Mockito.mock(ImpressionCounter.class);
        ImpressionObserver impressionObserver = new ImpressionObserver(200);

        ProcessImpressionStrategy processImpressionStrategy = new ProcessImpressionDebug(false, impressionObserver);
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(false, null, null);

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, processImpressionNone, processImpressionStrategy, impressionCounter, null);
        treatmentLog.start();

        // These 4 unique test name will cause 4 entries but we are caping at the first 3.
        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, null);
        KeyImpression ki2 = keyImpression("test2", "adil", "on", 2L, null);
        KeyImpression ki3 = keyImpression("test3", "pato", "on", 3L, null);
        KeyImpression ki4 = keyImpression("test4", "pato", "on", 4L, null);

        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, null, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, null, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, null, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, null, null), true)).collect(Collectors.toList()));

        // Do what the scheduler would do.
        treatmentLog.sendImpressions();

        verify(senderMock).postImpressionsBulk(impressionsCaptor.capture());

        List<TestImpressions> captured = impressionsCaptor.getValue();

        Assert.assertEquals(3, captured.size());
        verify(TELEMETRY_STORAGE, times(1)).recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_DROPPED, 1);
    }

    @Test
    public void works4ImpressionsInOneTest() {

        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ImpressionCounter impressionCounter = Mockito.mock(ImpressionCounter.class);
        ImpressionObserver impressionObserver = new ImpressionObserver(200);

        ProcessImpressionStrategy processImpressionStrategy = new ProcessImpressionDebug(false, impressionObserver);
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(false, null, null);

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, processImpressionNone, processImpressionStrategy, impressionCounter, null);
        treatmentLog.start();

        // These 4 unique test name will cause 4 entries but we are caping at the first 3.
        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki2 = keyImpression("test1", "adil", "on", 2L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 1L);
        KeyImpression ki4 = keyImpression("test1", "pato", "on", 4L, 1L);

        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, 1L, null), true)).collect(Collectors.toList()));

        // Do what the scheduler would do.
        treatmentLog.sendImpressions();

        verify(senderMock).postImpressionsBulk(impressionsCaptor.capture());

        List<TestImpressions> captured = impressionsCaptor.getValue();

        Assert.assertEquals(1, captured.size());
        Assert.assertEquals(4, captured.get(0).keyImpressions.size());
        Assert.assertEquals(ki1, captured.get(0).keyImpressions.get(0));
        verify(TELEMETRY_STORAGE, times(4)).recordImpressionStats(ImpressionsDataTypeEnum.IMPRESSIONS_QUEUED, 1);
    }

    @Test
    public void worksNoImpressions() {

        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ImpressionCounter impressionCounter = Mockito.mock(ImpressionCounter.class);
        ImpressionObserver impressionObserver = new ImpressionObserver(200);

        ProcessImpressionStrategy processImpressionStrategy = new ProcessImpressionDebug(false, impressionObserver);
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(false, null, null);

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, processImpressionNone, processImpressionStrategy, impressionCounter, null);

        // There are no impressions to post.

        // Do what the scheduler would do.
        treatmentLog.sendImpressions();

        verify(senderMock, never()).postImpressionsBulk(impressionsCaptor.capture());
    }

    @Test
    public void alreadySeenImpressionsAreMarked() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ImpressionCounter impressionCounter = Mockito.mock(ImpressionCounter.class);
        ImpressionObserver impressionObserver = new ImpressionObserver(200);

        ProcessImpressionStrategy processImpressionStrategy = new ProcessImpressionDebug(false, impressionObserver);
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(false, null, null);
        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, processImpressionNone, processImpressionStrategy, impressionCounter, null);
        treatmentLog.start();

        // These 4 unique test name will cause 4 entries but we are caping at the first 3.
        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki2 = keyImpression("test1", "adil2", "on", 2L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 1L);
        KeyImpression ki4 = keyImpression("test1", "pato2", "on", 4L, 1L);

        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.sendImpressions();

        verify(senderMock).postImpressionsBulk(impressionsCaptor.capture());

        List<TestImpressions> captured = impressionsCaptor.getValue();
        for (TestImpressions testImpressions : captured) {
            for (KeyImpression keyImpression : testImpressions.keyImpressions) {
                Assert.assertEquals(null, keyImpression.previousTime);
            }
        }

        // Do it again. Now they should all have a `seenAt` value
        Mockito.reset(senderMock);
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.sendImpressions();

        verify(senderMock).postImpressionsBulk(impressionsCaptor.capture());

        captured = impressionsCaptor.getAllValues().get(1);
        for (TestImpressions testImpressions : captured) {
            for (KeyImpression keyImpression : testImpressions.keyImpressions) {
                assertEquals(Optional.of(keyImpression.previousTime), Optional.of(keyImpression.time));
            }
        }
    }

    @Test
    public void testImpressionsStandaloneModeOptimizedMode() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.OPTIMIZED)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ImpressionCounter impressionCounter = new ImpressionCounter();
        ImpressionObserver impressionObserver = new ImpressionObserver(200);
        TelemetryStorageProducer telemetryStorageProducer = new InMemoryTelemetryStorage();

        ProcessImpressionStrategy processImpressionStrategy = new ProcessImpressionOptimized(false, impressionObserver, impressionCounter, telemetryStorageProducer);
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(false, null, null);

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, processImpressionNone, processImpressionStrategy, impressionCounter, null);
        treatmentLog.start();

        // These 4 unique test name will cause 4 entries but we are caping at the first 3.
        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki2 = keyImpression("test1", "adil", "on", 2L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 1L);
        KeyImpression ki4 = keyImpression("test1", "pato", "on", 4L, 1L);

        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.sendImpressions();

        verify(senderMock).postImpressionsBulk(impressionsCaptor.capture());

        List<TestImpressions> captured = impressionsCaptor.getValue();
        Assert.assertEquals(2, captured.get(0).keyImpressions.size());
        for (TestImpressions testImpressions : captured) {
            for (KeyImpression keyImpression : testImpressions.keyImpressions) {
                Assert.assertEquals(null, keyImpression.previousTime);
            }
        }
        // Only the first 2 impressions make it to the server
        Assert.assertTrue(captured.get(0).keyImpressions.contains(keyImpression("test1", "adil", "on", 1L, 1L)));
        Assert.assertTrue(captured.get(0).keyImpressions.contains(keyImpression("test1", "pato", "on", 3L, 1L)));

        treatmentLog.sendImpressionCounters();
        verify(senderMock).postCounters(impressionCountCaptor.capture());
        HashMap<ImpressionCounter.Key, Integer> capturedCounts = impressionCountCaptor.getValue();
        Assert.assertEquals(1, capturedCounts.size());
        Assert.assertTrue(capturedCounts.entrySet().contains(new AbstractMap.SimpleEntry<>(new ImpressionCounter.Key("test1", 0), 2)));

        // Assert that the sender is never called if the counters are empty.
        Mockito.reset(senderMock);
        treatmentLog.sendImpressionCounters();
        verify(senderMock, times(0)).postCounters(Mockito.any());
    }

    @Test
    public void testImpressionsStandaloneModeDebugMode() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ImpressionCounter impressionCounter = Mockito.mock(ImpressionCounter.class);
        ImpressionObserver impressionObserver = new ImpressionObserver(200);
        ProcessImpressionStrategy processImpressionStrategy = new ProcessImpressionDebug(false, impressionObserver);
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(false, null, null);

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, processImpressionNone, processImpressionStrategy, impressionCounter, null);
        treatmentLog.start();

        // These 4 unique test name will cause 4 entries but we are caping at the first 3.
        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki2 = keyImpression("test1", "adil", "on", 2L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 1L);
        KeyImpression ki4 = keyImpression("test1", "pato", "on", 4L, 1L);

        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.sendImpressions();

        verify(senderMock).postImpressionsBulk(impressionsCaptor.capture());

        List<TestImpressions> captured = impressionsCaptor.getValue();
        Assert.assertEquals(4, captured.get(0).keyImpressions.size());
        for (TestImpressions testImpressions : captured) {
            KeyImpression keyImpression1 = testImpressions.keyImpressions.get(0);
            KeyImpression keyImpression2 = testImpressions.keyImpressions.get(1);
            KeyImpression keyImpression3 = testImpressions.keyImpressions.get(2);
            KeyImpression keyImpression4 = testImpressions.keyImpressions.get(3);
            Assert.assertEquals(null, keyImpression1.previousTime);
            Assert.assertEquals(Optional.of(1L), Optional.of(keyImpression2.previousTime));
            Assert.assertEquals(null, keyImpression3.previousTime);
            Assert.assertEquals(Optional.of(3L), Optional.of(keyImpression4.previousTime));
        }
        // Only the first 2 impressions make it to the server
        Assert.assertTrue(captured.get(0).keyImpressions.contains(keyImpression("test1", "adil", "on", 1L, 1L)));
        Assert.assertTrue(captured.get(0).keyImpressions.contains(keyImpression("test1", "pato", "on", 3L, 1L)));
    }

    @Test
    public void testImpressionsStandaloneModeNoneMode() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.NONE)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);
        ImpressionCounter impressionCounter = new ImpressionCounter();
        UniqueKeysTracker uniqueKeysTracker = new UniqueKeysTrackerImp(telemetrySynchronizer, 1000, 1000, null);
        uniqueKeysTracker.start();

        ProcessImpressionStrategy processImpressionStrategy = new ProcessImpressionNone(false, uniqueKeysTracker, impressionCounter);
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(false, null, null);

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, processImpressionNone, processImpressionStrategy, impressionCounter, null);
        treatmentLog.start();

        // These 4 unique test name will cause 4 entries but we are caping at the first 3.
        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki2 = keyImpression("test1", "adil", "on", 2L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 1L);
        KeyImpression ki4 = keyImpression("test1", "pato", "on", 4L, 1L);

        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.close();
        uniqueKeysTracker.stop();

        verify(telemetrySynchronizer).synchronizeUniqueKeys(uniqueKeysCaptor.capture());

        List<UniqueKeys> uniqueKeysList = uniqueKeysCaptor.getAllValues();
        UniqueKeys uniqueKeys = uniqueKeysList.get(0);
        UniqueKeys.UniqueKey uniqueKey = uniqueKeys.uniqueKeys.get(0);
        Assert.assertEquals("test1", uniqueKey.featureName);

        List<String> keysDto = uniqueKey.keysDto;
        Assert.assertEquals("pato", keysDto.get(0));
        Assert.assertEquals("adil", keysDto.get(1));

        //treatmentLog.sendImpressionCounters();
        verify(senderMock).postCounters(impressionCountCaptor.capture());
        HashMap<ImpressionCounter.Key, Integer> capturedCounts = impressionCountCaptor.getValue();
        Assert.assertEquals(1, capturedCounts.size());
        Assert.assertTrue(capturedCounts.entrySet().contains(new AbstractMap.SimpleEntry<>(new ImpressionCounter.Key("test1", 0), 4)));

        // Assert that the sender is never called if the counters are empty.
        Mockito.reset(senderMock);
        treatmentLog.sendImpressionCounters();
        verify(senderMock, times(0)).postCounters(Mockito.any());
    }

    @Test
    public void testImpressionsConsumerModeOptimizedMode() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.OPTIMIZED)
                .operationMode(OperationMode.CONSUMER)
                .customStorageWrapper(Mockito.mock(CustomStorageWrapper.class))
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ImpressionCounter impressionCounter = new ImpressionCounter();
        ImpressionObserver impressionObserver = new ImpressionObserver(200);
        TelemetryStorageProducer telemetryStorageProducer = new InMemoryTelemetryStorage();

        ProcessImpressionStrategy processImpressionStrategy = new ProcessImpressionOptimized(false, impressionObserver, impressionCounter, telemetryStorageProducer);
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(false, null, null);
        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, processImpressionNone, processImpressionStrategy, impressionCounter, null);
        treatmentLog.start();

        // These 4 unique test name will cause 4 entries but we are caping at the first 3.
        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki2 = keyImpression("test1", "adil", "on", 2L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 1L);
        KeyImpression ki4 = keyImpression("test1", "pato", "on", 4L, 1L);

        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.sendImpressions();

        verify(senderMock).postImpressionsBulk(impressionsCaptor.capture());

        List<TestImpressions> captured = impressionsCaptor.getValue();
        Assert.assertEquals(2, captured.get(0).keyImpressions.size());
        for (TestImpressions testImpressions : captured) {
            for (KeyImpression keyImpression : testImpressions.keyImpressions) {
                Assert.assertEquals(null, keyImpression.previousTime);
            }
        }
        // Only the first 2 impressions make it to the server
        Assert.assertTrue(captured.get(0).keyImpressions.contains(keyImpression("test1", "adil", "on", 1L, 1L)));
        Assert.assertTrue(captured.get(0).keyImpressions.contains(keyImpression("test1", "pato", "on", 3L, 1L)));

        treatmentLog.sendImpressionCounters();
        verify(senderMock).postCounters(impressionCountCaptor.capture());
        HashMap<ImpressionCounter.Key, Integer> capturedCounts = impressionCountCaptor.getValue();
        Assert.assertEquals(1, capturedCounts.size());
        Assert.assertTrue(capturedCounts.entrySet().contains(new AbstractMap.SimpleEntry<>(new ImpressionCounter.Key("test1", 0), 2)));

        // Assert that the sender is never called if the counters are empty.
        Mockito.reset(senderMock);
        treatmentLog.sendImpressionCounters();
        verify(senderMock, times(0)).postCounters(Mockito.any());
    }

    @Test
    public void testImpressionsConsumerModeNoneMode() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.NONE)
                .operationMode(OperationMode.CONSUMER)
                .customStorageWrapper(Mockito.mock(CustomStorageWrapper.class))
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);
        ImpressionCounter impressionCounter = new ImpressionCounter();
        UniqueKeysTracker uniqueKeysTracker = new UniqueKeysTrackerImp(telemetrySynchronizer, 1000, 1000, null);
        uniqueKeysTracker.start();
        ProcessImpressionStrategy processImpressionStrategy = new ProcessImpressionNone(false, uniqueKeysTracker, impressionCounter);
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(false, null, null);

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, processImpressionNone, processImpressionStrategy, impressionCounter, null);
        treatmentLog.start();

        // These 4 unique test name will cause 4 entries but we are caping at the first 3.
        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki2 = keyImpression("test1", "adil", "on", 2L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 1L);
        KeyImpression ki4 = keyImpression("test1", "pato", "on", 4L, 1L);

        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, 1L, null), true)).collect(Collectors.toList()));
        uniqueKeysTracker.stop();
        treatmentLog.close();

        verify(telemetrySynchronizer).synchronizeUniqueKeys(uniqueKeysCaptor.capture());

        List<UniqueKeys> uniqueKeysList = uniqueKeysCaptor.getAllValues();
        UniqueKeys uniqueKeys = uniqueKeysList.get(0);
        UniqueKeys.UniqueKey uniqueKey = uniqueKeys.uniqueKeys.get(0);
        Assert.assertEquals("test1", uniqueKey.featureName);

        List<String> keysDto = uniqueKey.keysDto;
        Assert.assertEquals("pato", keysDto.get(0));
        Assert.assertEquals("adil", keysDto.get(1));

        //treatmentLog.sendImpressionCounters();
        verify(senderMock).postCounters(impressionCountCaptor.capture());
        HashMap<ImpressionCounter.Key, Integer> capturedCounts = impressionCountCaptor.getValue();
        Assert.assertEquals(1, capturedCounts.size());
        Assert.assertTrue(capturedCounts.entrySet().contains(new AbstractMap.SimpleEntry<>(new ImpressionCounter.Key("test1", 0), 4)));

        // Assert that the sender is never called if the counters are empty.
        Mockito.reset(senderMock);
        treatmentLog.sendImpressionCounters();
        verify(senderMock, times(0)).postCounters(Mockito.any());
    }

    @Test
    public void testImpressionsConsumerModeDebugMode() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .operationMode(OperationMode.CONSUMER)
                .customStorageWrapper(Mockito.mock(CustomStorageWrapper.class))
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ImpressionCounter impressionCounter = Mockito.mock(ImpressionCounter.class);
        ImpressionObserver impressionObserver = new ImpressionObserver(200);
        ProcessImpressionStrategy processImpressionStrategy = new ProcessImpressionDebug(false, impressionObserver);
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(false, null, null);

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, processImpressionNone, processImpressionStrategy, impressionCounter, null);
        treatmentLog.start();

        // These 4 unique test name will cause 4 entries but we are caping at the first 3.
        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki2 = keyImpression("test1", "adil", "on", 2L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 1L);
        KeyImpression ki4 = keyImpression("test1", "pato", "on", 4L, 1L);

        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.sendImpressions();

        verify(senderMock).postImpressionsBulk(impressionsCaptor.capture());

        List<TestImpressions> captured = impressionsCaptor.getValue();
        Assert.assertEquals(4, captured.get(0).keyImpressions.size());
        for (TestImpressions testImpressions : captured) {
            KeyImpression keyImpression1 = testImpressions.keyImpressions.get(0);
            KeyImpression keyImpression2 = testImpressions.keyImpressions.get(1);
            KeyImpression keyImpression3 = testImpressions.keyImpressions.get(2);
            KeyImpression keyImpression4 = testImpressions.keyImpressions.get(3);
            Assert.assertEquals(null, keyImpression1.previousTime);
            Assert.assertEquals(Optional.of(1L), Optional.of(keyImpression2.previousTime));
            Assert.assertEquals(null, keyImpression3.previousTime);
            Assert.assertEquals(Optional.of(3L), Optional.of(keyImpression4.previousTime));
        }
        // Only the first 2 impressions make it to the server
        Assert.assertTrue(captured.get(0).keyImpressions.contains(keyImpression("test1", "adil", "on", 1L, 1L)));
        Assert.assertTrue(captured.get(0).keyImpressions.contains(keyImpression("test1", "pato", "on", 3L, 1L)));
    }

    @Test
    public void testCounterStandaloneModeOptimizedMode() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.OPTIMIZED)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ImpressionCounter impressionCounter = Mockito.mock(ImpressionCounter.class);
        ImpressionObserver impressionObserver = new ImpressionObserver(200);
        TelemetryStorageProducer telemetryStorageProducer = new InMemoryTelemetryStorage();

        ProcessImpressionStrategy processImpressionStrategy = new ProcessImpressionOptimized(false, impressionObserver, impressionCounter, telemetryStorageProducer);
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(false, null, null);
        ImpressionsManagerImpl manager = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, processImpressionNone, processImpressionStrategy, impressionCounter, null);
        manager.start();
        Assert.assertNotNull(manager.getCounter());
    }
    @Test
    public void testCounterStandaloneModeDebugMode() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ImpressionObserver impressionObserver = new ImpressionObserver(200);
        ProcessImpressionStrategy processImpressionStrategy = new ProcessImpressionDebug(false, impressionObserver);
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(false, null, null);

        ImpressionsManagerImpl manager = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, processImpressionNone, processImpressionStrategy, null, null);
        manager.start();
        Assert.assertNull(manager.getCounter());
    }

    @Test
    public void testCounterStandaloneModeNoneMode() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.NONE)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ProcessImpressionStrategy processImpressionStrategy = Mockito.mock(ProcessImpressionNone.class);
        ImpressionCounter impressionCounter = Mockito.mock(ImpressionCounter.class);

        ImpressionsManagerImpl manager = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, Mockito.mock(ProcessImpressionNone.class), processImpressionStrategy, impressionCounter, null);
        manager.start();
        Assert.assertNotNull(manager.getCounter());
    }

    @Test
    public void testCounterConsumerModeOptimizedMode() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.OPTIMIZED)
                .operationMode(OperationMode.CONSUMER)
                .customStorageWrapper(Mockito.mock(CustomStorageWrapper.class))
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ProcessImpressionStrategy processImpressionStrategy = Mockito.mock(ProcessImpressionOptimized.class);
        ImpressionCounter impressionCounter = Mockito.mock(ImpressionCounter.class);

        ImpressionsManagerImpl manager = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, Mockito.mock(ProcessImpressionNone.class), processImpressionStrategy, impressionCounter, null);
        manager.start();
        Assert.assertNotNull(manager.getCounter());
    }

    @Test
    public void testCounterConsumerModeDebugMode() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .operationMode(OperationMode.CONSUMER)
                .customStorageWrapper(Mockito.mock(CustomStorageWrapper.class))
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ProcessImpressionStrategy processImpressionStrategy = Mockito.mock(ProcessImpressionDebug.class);

        ImpressionsManagerImpl manager = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, Mockito.mock(ProcessImpressionNone.class), processImpressionStrategy, null, null);
        manager.start();
        Assert.assertNull(manager.getCounter());
    }

    @Test
    public void testCounterConsumerModeNoneMode() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.NONE)
                .operationMode(OperationMode.CONSUMER)
                .customStorageWrapper(Mockito.mock(CustomStorageWrapper.class))
                .build();

        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ProcessImpressionStrategy processImpressionStrategy = Mockito.mock(ProcessImpressionNone.class);
        ImpressionCounter impressionCounter = Mockito.mock(ImpressionCounter.class);

        ImpressionsManagerImpl manager = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, Mockito.mock(ProcessImpressionNone.class), processImpressionStrategy, impressionCounter, null);
        manager.start();
        Assert.assertNotNull(manager.getCounter());
    }

    @Test
    public void testImpressionToggleStandaloneOptimizedMode() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.OPTIMIZED)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ImpressionCounter impressionCounter = new ImpressionCounter();
        ImpressionObserver impressionObserver = new ImpressionObserver(200);
        TelemetryStorageProducer telemetryStorageProducer = new InMemoryTelemetryStorage();
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);
        UniqueKeysTracker uniqueKeysTracker = new UniqueKeysTrackerImp(telemetrySynchronizer, 1000, 1000, null);
        uniqueKeysTracker.start();

        ProcessImpressionStrategy processImpressionStrategy = new ProcessImpressionOptimized(false, impressionObserver, impressionCounter, telemetryStorageProducer);
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(false, uniqueKeysTracker, impressionCounter);

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, processImpressionNone, processImpressionStrategy, impressionCounter, null);
        treatmentLog.start();

        // These 4 unique test name will cause 4 entries but we are caping at the first 3.
        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki2 = keyImpression("test1", "mati", "on", 2L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 1L);
        KeyImpression ki4 = keyImpression("test1", "bilal", "on", 4L, 1L);

        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null), false)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, 1L, null), false)).collect(Collectors.toList()));
        treatmentLog.sendImpressions();

        verify(senderMock).postImpressionsBulk(impressionsCaptor.capture());

        List<TestImpressions> captured = impressionsCaptor.getValue();
        Assert.assertEquals(2, captured.get(0).keyImpressions.size());
        for (TestImpressions testImpressions : captured) {
            for (KeyImpression keyImpression : testImpressions.keyImpressions) {
                Assert.assertEquals(null, keyImpression.previousTime);
            }
        }
        // Only the first 2 impressions make it to the server
        Assert.assertTrue(captured.get(0).keyImpressions.contains(keyImpression("test1", "adil", "on", 1L, 1L)));
        Assert.assertTrue(captured.get(0).keyImpressions.contains(keyImpression("test1", "pato", "on", 3L, 1L)));

        HashMap<String, HashSet<String>> trackedKeys = ((UniqueKeysTrackerImp) uniqueKeysTracker).popAll();
        HashSet<String> keys = new HashSet<>();
        keys.add("mati");
        keys.add("bilal");
        Assert.assertEquals(1, trackedKeys.size());
        Assert.assertEquals(keys, trackedKeys.get("test1"));

        treatmentLog.sendImpressionCounters();
        verify(senderMock).postCounters(impressionCountCaptor.capture());
        HashMap<ImpressionCounter.Key, Integer> capturedCounts = impressionCountCaptor.getValue();
        Assert.assertEquals(1, capturedCounts.size());
        Assert.assertTrue(capturedCounts.entrySet().contains(new AbstractMap.SimpleEntry<>(new ImpressionCounter.Key("test1", 0), 2)));

        // Assert that the sender is never called if the counters are empty.
        Mockito.reset(senderMock);
        treatmentLog.sendImpressionCounters();
        verify(senderMock, times(0)).postCounters(Mockito.any());
    }

    @Test
    public void testImpressionToggleStandaloneModeDebugMode() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.DEBUG)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ImpressionCounter impressionCounter = Mockito.mock(ImpressionCounter.class);
        ImpressionObserver impressionObserver = new ImpressionObserver(200);
        ProcessImpressionStrategy processImpressionStrategy = new ProcessImpressionDebug(false, impressionObserver);
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);
        UniqueKeysTracker uniqueKeysTracker = new UniqueKeysTrackerImp(telemetrySynchronizer, 1000, 1000, null);
        uniqueKeysTracker.start();
        ProcessImpressionNone processImpressionNone = new ProcessImpressionNone(false, uniqueKeysTracker, impressionCounter);

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, processImpressionNone, processImpressionStrategy, impressionCounter, null);
        treatmentLog.start();

        // These 4 unique test name will cause 4 entries but we are caping at the first 3.
        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki2 = keyImpression("test1", "mati", "on", 2L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 1L);
        KeyImpression ki4 = keyImpression("test1", "bilal", "on", 4L, 1L);

        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null), false)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, 1L, null), false)).collect(Collectors.toList()));
        treatmentLog.sendImpressions();

        HashMap<String, HashSet<String>> trackedKeys = ((UniqueKeysTrackerImp) uniqueKeysTracker).popAll();
        HashSet<String> keys = new HashSet<>();
        keys.add("mati");
        keys.add("bilal");
        Assert.assertEquals(1, trackedKeys.size());
        Assert.assertEquals(keys, trackedKeys.get("test1"));

        verify(senderMock).postImpressionsBulk(impressionsCaptor.capture());

        List<TestImpressions> captured = impressionsCaptor.getValue();
        Assert.assertEquals(2, captured.get(0).keyImpressions.size());
        for (TestImpressions testImpressions : captured) {
            KeyImpression keyImpression1 = testImpressions.keyImpressions.get(0);
            KeyImpression keyImpression3 = testImpressions.keyImpressions.get(1);
            Assert.assertEquals(null, keyImpression1.previousTime);
            Assert.assertEquals(null, keyImpression3.previousTime);
        }
        // Only the first 2 impressions make it to the server
        Assert.assertTrue(captured.get(0).keyImpressions.contains(keyImpression("test1", "adil", "on", 1L, 1L)));
        Assert.assertTrue(captured.get(0).keyImpressions.contains(keyImpression("test1", "pato", "on", 3L, 1L)));
    }

    @Test
    public void testImpressionToggleStandaloneModeNoneMode() {
        SplitClientConfig config = SplitClientConfig.builder()
                .impressionsQueueSize(10)
                .endpoint("nowhere.com", "nowhere.com")
                .impressionsMode(ImpressionsManager.Mode.NONE)
                .build();
        ImpressionsStorage storage = new InMemoryImpressionsStorage(config.impressionsQueueSize());

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);
        ImpressionCounter impressionCounter = new ImpressionCounter();
        UniqueKeysTracker uniqueKeysTracker = new UniqueKeysTrackerImp(telemetrySynchronizer, 1000, 1000, null);
        uniqueKeysTracker.start();

        ProcessImpressionStrategy processImpressionStrategy = new ProcessImpressionNone(false, uniqueKeysTracker, impressionCounter);
        ProcessImpressionNone processImpressionNone = (ProcessImpressionNone) processImpressionStrategy;

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(config, senderMock, TELEMETRY_STORAGE, storage, storage, processImpressionNone, processImpressionStrategy, impressionCounter, null);
        treatmentLog.start();

        // These 4 unique test name will cause 4 entries but we are caping at the first 3.
        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki2 = keyImpression("test1", "mati", "on", 2L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 1L);
        KeyImpression ki4 = keyImpression("test1", "bilal", "on", 4L, 1L);

        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null), false)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null), true)).collect(Collectors.toList()));
        treatmentLog.track(Stream.of(new DecoratedImpression(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, 1L, null), false)).collect(Collectors.toList()));
        treatmentLog.close();
        HashMap<String, HashSet<String>> trackedKeys = ((UniqueKeysTrackerImp) uniqueKeysTracker).popAll();
        uniqueKeysTracker.stop();

        HashSet<String> keys = new HashSet<>();
        keys.add("adil");
        keys.add("mati");
        keys.add("pato");
        keys.add("bilal");
        Assert.assertEquals(1, trackedKeys.size());
        Assert.assertEquals(keys, trackedKeys.get("test1"));

        //treatmentLog.sendImpressionCounters();
        verify(senderMock).postCounters(impressionCountCaptor.capture());
        HashMap<ImpressionCounter.Key, Integer> capturedCounts = impressionCountCaptor.getValue();
        Assert.assertEquals(1, capturedCounts.size());
        Assert.assertTrue(capturedCounts.entrySet().contains(new AbstractMap.SimpleEntry<>(new ImpressionCounter.Key("test1", 0), 4)));

        // Assert that the sender is never called if the counters are empty.
        Mockito.reset(senderMock);
        treatmentLog.sendImpressionCounters();
        verify(senderMock, times(0)).postCounters(Mockito.any());
    }
}