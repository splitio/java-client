package io.split.engine.sse.workers;

import io.split.engine.experiments.SplitFetcher;
import org.junit.Test;
import org.mockito.Mockito;

public class SplitsWorkerTest {

    @Test
    public void addToQueueWithoutElementsWShouldNotTriggerFetch() throws InterruptedException {
        SplitFetcher splitFetcherMock = Mockito.mock(SplitFetcher.class);

        SplitsWorker splitsWorker = new SplitsWorkerImp(splitFetcherMock);
        Thread thread = new Thread(splitsWorker);
        thread.start();

        Thread.sleep(500);
        Mockito.verify(splitFetcherMock, Mockito.never()).changeNumber();
        Mockito.verify(splitFetcherMock, Mockito.never()).forceRefresh();

        thread.interrupt();
    }

    @Test
    public void addToQueueWithElementsWShouldTriggerFetch() throws InterruptedException {
        SplitFetcher splitFetcherMock = Mockito.mock(SplitFetcher.class);

        Mockito.when(splitFetcherMock.changeNumber())
                .thenReturn(1585956698447L)
                .thenReturn(1585956698457L)
                .thenReturn(1585956698467L)
                .thenReturn(1585956698477L);

        SplitsWorker splitsWorker = new SplitsWorkerImp(splitFetcherMock);
        Thread thread = new Thread(splitsWorker);
        thread.start();

        splitsWorker.addToQueue(1585956698457L);
        splitsWorker.addToQueue(1585956698467L);
        splitsWorker.addToQueue(1585956698477L);
        splitsWorker.addToQueue(1585956698476L);
        Thread.sleep(1000);

        Mockito.verify(splitFetcherMock, Mockito.times(4)).changeNumber();
        Mockito.verify(splitFetcherMock, Mockito.times(3)).forceRefresh();

        thread.interrupt();
    }

    @Test
    public void killShouldTriggerFetch() {
        long changeNumber = 1585956698457L;
        String splitName = "split-test";
        String defaultTreatment = "off";

        SplitFetcher splitFetcherMock = Mockito.mock(SplitFetcher.class);

        SplitsWorker splitsWorker = new SplitsWorkerImp(splitFetcherMock);
        Thread thread = new Thread(splitsWorker);
        thread.start();

        splitsWorker.killSplit(changeNumber, splitName, defaultTreatment);

        Mockito.verify(splitFetcherMock, Mockito.times(1)).killSplit(splitName, defaultTreatment, changeNumber);
    }
}
