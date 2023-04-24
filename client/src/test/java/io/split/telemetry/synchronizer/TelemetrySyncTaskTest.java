package io.split.telemetry.synchronizer;

import io.split.client.SplitClientConfig;
import org.junit.Test;
import org.mockito.Mockito;

public class TelemetrySyncTaskTest {
    private static SplitClientConfig config = Mockito.mock(SplitClientConfig.class);

    @Test
    public void testSynchronizationTask() throws Exception {
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);
        Mockito.doNothing().when(telemetrySynchronizer).synchronizeStats();
        TelemetrySyncTask telemetrySyncTask = new TelemetrySyncTask(1, telemetrySynchronizer, config);
        telemetrySyncTask.startScheduledTask();
        Thread.sleep(2900);
        Mockito.verify(telemetrySynchronizer, Mockito.times(2)).synchronizeStats();
    }

    @Test
    public void testStopSynchronizationTask() throws Exception {
        TelemetrySynchronizer telemetrySynchronizer = Mockito.mock(TelemetryInMemorySubmitter.class);
//        Mockito.doNothing().when(telemetrySynchronizer).synchronizeStats();
        TelemetrySyncTask telemetrySyncTask = new TelemetrySyncTask(1, telemetrySynchronizer, config);
        telemetrySyncTask.startScheduledTask();
        Thread.sleep(2100);
        Mockito.verify(telemetrySynchronizer, Mockito.times(2)).synchronizeStats();
        telemetrySyncTask.stopScheduledTask();
        Mockito.verify(telemetrySynchronizer, Mockito.times(2)).synchronizeStats();
        Mockito.verify(telemetrySynchronizer, Mockito.times(1)).finalSynchronization();
    }

}