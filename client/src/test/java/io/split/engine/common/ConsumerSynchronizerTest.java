package io.split.engine.common;

import io.split.client.impressions.ImpressionsManager;
import io.split.client.impressions.UniqueKeysTracker;
import io.split.telemetry.synchronizer.TelemetrySyncTask;
import org.junit.Test;
import org.mockito.Mockito;

public class ConsumerSynchronizerTest {

    @Test
    public void testDataRecording() {
        ImpressionsManager impressionsManager = Mockito.mock(ImpressionsManager.class);
        UniqueKeysTracker uniqueKeysTracker = Mockito.mock(UniqueKeysTracker.class);
        TelemetrySyncTask telemetrySyncTask = Mockito.mock(TelemetrySyncTask.class);
        SplitTasks splitTasks = SplitTasks.build(null, null, impressionsManager, null, telemetrySyncTask, uniqueKeysTracker);
        Synchronizer imp = new ConsumerSynchronizer(splitTasks);
        imp.startPeriodicDataRecording();

        Mockito.verify(impressionsManager, Mockito.times(1)).start();
        Mockito.verify(uniqueKeysTracker, Mockito.times(1)).start();
        Mockito.verify(telemetrySyncTask, Mockito.times(1)).startScheduledTask();

        imp.stopPeriodicDataRecording(3L, 1L, 1L);

        Mockito.verify(impressionsManager, Mockito.times(1)).close();
        Mockito.verify(uniqueKeysTracker, Mockito.times(1)).stop();
        Mockito.verify(telemetrySyncTask, Mockito.times(1)).stopScheduledTask(3L,1L,1L);
    }
}