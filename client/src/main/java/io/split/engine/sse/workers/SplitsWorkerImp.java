package io.split.engine.sse.workers;

import io.split.engine.experiments.SplitFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

public class SplitsWorkerImp implements SplitsWorker {
    private static final Logger _log = LoggerFactory.getLogger(SplitsWorker.class);

    private final SplitFetcher _splitFetcher;
    private final LinkedBlockingQueue<Long> _queue;
    private Thread _thread;

    public SplitsWorkerImp(SplitFetcher splitFetcher) {
        _splitFetcher = splitFetcher;
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
    public void start() {
        _log.debug("Splits Worker starting ...");
        _thread = new Thread(this);
        _thread.start();
        _queue.clear();
    }

    @Override
    public void stop() {
        _thread.interrupt();
        _queue.clear();
        _log.debug("Splits Worked stopped.");
    }

    @Override
    public void run() {
        try {
            while (!_thread.isInterrupted()) {
                Long changeNumber = _queue.take();

                if (changeNumber != null) {
                    _log.debug(String.format("changeNumber dequeue: %s", changeNumber));

                    if (changeNumber > _splitFetcher.changeNumber()) {
                        _splitFetcher.forceRefresh();
                    }
                }
            }
        } catch (InterruptedException ex) {
            _log.debug("The thread was stopped.");
        } catch (Exception ex) {
            _log.error(ex.getMessage());
        }
    }
}
