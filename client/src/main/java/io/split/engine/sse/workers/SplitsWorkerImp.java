package io.split.engine.sse.workers;

import io.split.engine.experiments.SplitFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SplitsWorkerImp implements SplitsWorker {
    private static final int QUEUE_READ_TIMEOUT_SECONDS = 5;
    private static final Logger _log = LoggerFactory.getLogger(SplitsWorker.class);

    private final SplitFetcher _splitFetcher;
    private final AtomicBoolean _stop;
    private final LinkedBlockingQueue<Long> _queue;

    public SplitsWorkerImp(SplitFetcher splitFetcher) {
        _splitFetcher = splitFetcher;
        _stop = new AtomicBoolean(false);
        _queue = new LinkedBlockingQueue<>();
    }

    @Override
    public void addToQueue(long changeNumber) {
        try {
            _queue.add(changeNumber);
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
    public void stop() {
        _stop.set(true);
    }

    @Override
    public void run() {
        try {
            _stop.set(false);

            while(!_stop.get()) {
                Long changeNumber = _queue.poll(QUEUE_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (changeNumber != null) {
                    _log.debug(String.format("changeNumber dequeue: %s", changeNumber));

                    if (changeNumber > _splitFetcher.changeNumber()) {
                        _splitFetcher.forceRefresh();
                    }
                }
            }
        } catch (Exception ex) {
            _log.error(ex.getMessage());
        }
    }
}
