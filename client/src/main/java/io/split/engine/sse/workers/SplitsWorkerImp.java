package io.split.engine.sse.workers;

import io.split.engine.experiments.SplitFetcher;
import io.split.engine.sse.queues.SplitNotificationsQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplitsWorkerImp implements SplitsWorker {
    private static final Logger _log = LoggerFactory.getLogger(SplitsWorker.class);

    private final SplitFetcher _splitFetcher;

    public SplitsWorkerImp(SplitFetcher splitFetcher) {
        _splitFetcher = splitFetcher;
    }

    @Override
    public void addToQueue(long changeNumber) {
        try {
            SplitNotificationsQueue.queue.add(changeNumber);
            _log.debug(String.format("Added to split queue: %s", changeNumber));
        } catch (Exception ex) {
            _log.error(String.format("Exception on SplitWorker addToQueue: %s", ex.getMessage()));
        }
    }

    @Override
    public void killSplit(long changeNumber, String splitName, String defaultTreatment) {
        try {
            _splitFetcher.killSplit(splitName, defaultTreatment, changeNumber);
            _log.debug(String.format("Kill split: %s, changeNumber: %s, defaultTreatment: %s", splitName, changeNumber, defaultTreatment));
        } catch (Exception ex) {
            _log.error(String.format("Exception on SplitWorker killSplit: %s", ex.getMessage()));
        }
    }

    @Override
    public void run() {
        try {
            while(true) {
                long changeNumber = SplitNotificationsQueue.queue.take();
                _log.debug(String.format("changeNumber dequeue: %s", changeNumber));

                if (changeNumber > _splitFetcher.changeNumber()) {
                    _splitFetcher.forceRefresh();
                }
            }
        } catch (Exception ex) {
            _log.error(ex.getMessage());
        }
    }
}
