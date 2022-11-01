package io.split.engine.common;

import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

public class ConsumerSyncManagerTest {
    @Test
    public void testStartAndShutdown() throws IOException {
        Synchronizer redisSynchronizer = Mockito.mock(ConsumerSynchronizer.class);
        ConsumerSyncManager imp = new ConsumerSyncManager(redisSynchronizer);
        imp.start();
        Mockito.verify(redisSynchronizer, Mockito.times(1)).startPeriodicDataRecording();
        imp.shutdown(3L,1L,1L);
        Mockito.verify(redisSynchronizer, Mockito.times(1)).stopPeriodicDataRecording(3L,1L,1L);
    }
}