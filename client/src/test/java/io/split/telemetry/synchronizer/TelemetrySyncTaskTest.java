package io.split.telemetry.synchronizer;

import org.junit.Test;
import org.mockito.Mockito;

public class TelemetrySyncTaskTest {

    @Test
    public void testSynchronizationTask() throws Exception {
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(SynchronizerMemory.class);
        TelemetrySyncTask telemetrySyncTask = new TelemetrySyncTask(1, telemetrySynchronizer);
        telemetrySyncTask.startScheduledTask();
        Thread.sleep(3000);
        Mockito.verify(telemetrySynchronizer, Mockito.times(3)).synchronizeStats();
    }

}