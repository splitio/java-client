package io.split.engine.sse.workers;

import io.split.engine.common.Synchronizer;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

public class SplitsWorkerTest {

    @Test
    public void addToQueueWithoutElementsWShouldNotTriggerFetch() throws InterruptedException {
        Synchronizer splitFetcherMock = Mockito.mock(Synchronizer.class);

        SplitsWorker splitsWorker = new SplitsWorkerImp(splitFetcherMock);
        splitsWorker.start();

        Thread.sleep(500);
        Mockito.verify(splitFetcherMock, Mockito.never()).refreshSplits(Mockito.anyLong());
        splitsWorker.stop();
    }

    @Test
    public void addToQueueWithElementsWShouldTriggerFetch() throws InterruptedException {
        Synchronizer syncMock = Mockito.mock(Synchronizer.class);

        SplitsWorker splitsWorker = new SplitsWorkerImp(syncMock);
        splitsWorker.start();

        ArgumentCaptor<Long> cnCaptor = ArgumentCaptor.forClass(Long.class);
        splitsWorker.addToQueue(1585956698457L);
        splitsWorker.addToQueue(1585956698467L);
        splitsWorker.addToQueue(1585956698477L);
        splitsWorker.addToQueue(1585956698476L);
        Thread.sleep(1000);

        Mockito.verify(syncMock, Mockito.times(4)).refreshSplits(cnCaptor.capture());
        List<Long> captured = cnCaptor.getAllValues();
        assertThat(captured, contains(1585956698457L, 1585956698467L, 1585956698477L, 1585956698476L));
        splitsWorker.stop();
    }

    @Test
    public void killShouldTriggerFetch() {
        long changeNumber = 1585956698457L;
        String splitName = "split-test";
        String defaultTreatment = "off";

        Synchronizer syncMock = Mockito.mock(Synchronizer.class);
        SplitsWorker splitsWorker = new SplitsWorkerImp(syncMock);
        splitsWorker.start();

        splitsWorker.killSplit(changeNumber, splitName, defaultTreatment);
        Mockito.verify(syncMock, Mockito.times(1)).localKillSplit(splitName, defaultTreatment, changeNumber);
        splitsWorker.stop();
    }

    @Test
    public void messagesNotProcessedWhenWorkerStopped() throws InterruptedException {
        Synchronizer syncMock = Mockito.mock(Synchronizer.class);
        SplitsWorker splitsWorker = new SplitsWorkerImp(syncMock);
        splitsWorker.start();
        splitsWorker.addToQueue(1585956698457L);
        Thread.sleep(500);


        splitsWorker.stop();
        Thread.sleep(500);

        splitsWorker.addToQueue(1585956698467L);
        Mockito.verify(syncMock, Mockito.times(1)).refreshSplits(1585956698457L); // Previous one!

        Mockito.reset(syncMock);
        splitsWorker.start();
        splitsWorker.addToQueue(1585956698477L);
        Thread.sleep(500);
        Mockito.verify(syncMock, Mockito.times(1)).refreshSplits(1585956698477L);
        splitsWorker.stop();
    }
}
