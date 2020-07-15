package io.split.engine.sse.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Worker<T> implements Runnable {
    protected static final Logger _log = LoggerFactory.getLogger(Worker.class);

    private final String _workerName;
    protected final LinkedBlockingQueue<T> _queue;
    protected AtomicBoolean _running;
    protected Thread _thread;

    public Worker(String workerName) {
        _queue = new LinkedBlockingQueue<>();
        _workerName = workerName;
        _running = new AtomicBoolean(false);
    }

    public void start() {
        if (_running.get()) {
            _log.error(String.format("%s Worker already running.", _workerName));
            return;
        }

        _log.debug(String.format("%s Worker starting ...", _workerName));
        _queue.clear();
        _running.set(true);
        _thread = new Thread( this);
        _thread.start();
    }

    public void stop() {
        if (!_running.get()) {
            _log.error(String.format("%s Worker not running.", _workerName));
            return;
        }

        _running.set(false);
        _thread.interrupt();
        _log.debug(String.format("%s Worked stopped.", _workerName));
    }

    public void addToQueue(T element) {
        try {
            _queue.add(element);
            _log.debug(String.format("Added to %s queue: %s", _workerName, element.toString()));
        } catch (Exception ex) {
            _log.error(String.format("Exception on %s Worker addToQueue: %s", _workerName, ex.getMessage()));
        }
    }

    @Override
    public void run() {
        while (_running.get()) {
            T element = null;

            try {
                element = _queue.take();
            }  catch (InterruptedException ex) {
                _log.debug("The thread was stopped.");
                Thread.currentThread().interrupt();
                break;
            }

            if (element == null) {
                continue;
            }

            _log.debug(String.format("Dequeue: %s", element.toString()));

            executeRefresh(element);
        }
    }

    protected abstract void executeRefresh(T element);
}
