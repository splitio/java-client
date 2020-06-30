package io.split.engine.sse.workers;

import io.split.engine.experiments.SplitFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class SplitsWorkerImp implements SplitsWorker {
    private static final Logger _log = LoggerFactory.getLogger(SplitsWorker.class);

    private final SplitFetcher _splitFetcher;
    private final LinkedBlockingQueue<Long> _queue;
    private AtomicBoolean _running;
    private Thread _thread;

    public SplitsWorkerImp(SplitFetcher splitFetcher) {
        _splitFetcher = splitFetcher;
        _queue = new LinkedBlockingQueue<>();
        _running = new AtomicBoolean(false);
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
        if (_running.get()) {
            _log.error("Split Worker already running.");
            return;
        }

        _log.debug("Splits Worker starting ...");
        _queue.clear();
        _running.set(true);
        _thread = new Thread(this);
        _thread.start();
    }

    @Override
    public void stop() {
        if (!_running.get()) {
            _log.error("Split Worker not running.");
            return;
        }

        _running.set(false);
        _thread.interrupt();
        _log.debug("Splits Worked stopped.");
    }

    @Override
    public void run() {
        while (_running.get()) {
            Long changeNumber = null;

            try {
                changeNumber = _queue.take();
            }  catch (InterruptedException ex) {
                _log.debug("The thread was stopped.");
                Thread.currentThread().interrupt();
                break;
            }

            if (changeNumber == null) {
                continue;
            }

            _log.debug(String.format("changeNumber dequeue: %s", changeNumber));

            if (changeNumber > _splitFetcher.changeNumber()) {
                _splitFetcher.forceRefresh();
            }
        }
    }
}
