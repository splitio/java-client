package io.split.telemetry.synchronizer;

import org.junit.Test;
import org.mockito.Mockito;

public class TelemetrySyncTaskTest {

    @Test
    public void testSynchronizationTask() throws Exception {
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(SynchronizerMemory.class);
        Mockito.doNothing().when(telemetrySynchronizer).synchronizeStats();
        TelemetrySyncTask telemetrySyncTask = new TelemetrySyncTask(1, telemetrySynchronizer);
        Thread.sleep(2900);
        Mockito.verify(telemetrySynchronizer, Mockito.times(3)).synchronizeStats();
    }

    @Test
    public void testStopSynchronizationTask() throws Exception {
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(SynchronizerMemory.class);
//        Mockito.doNothing().when(telemetrySynchronizer).synchronizeStats();
        TelemetrySyncTask telemetrySyncTask = new TelemetrySyncTask(1, telemetrySynchronizer);
        Thread.sleep(3000);
        Mockito.verify(telemetrySynchronizer, Mockito.times(3)).synchronizeStats();
        telemetrySyncTask.stopScheduledTask();
        Thread.sleep(2000);
        Mockito.verify(telemetrySynchronizer, Mockito.times(4)).synchronizeStats();

    }

}