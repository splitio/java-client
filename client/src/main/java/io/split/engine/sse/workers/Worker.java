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
        if (_running.compareAndSet(false, true)) {
            _log.debug(String.format("%s Worker starting ...", _workerName));
            _queue.clear();
            _thread = new Thread( this);
            _thread.setName(String.format("%s-worker", _workerName));
            _thread.start();
            _log.debug(String.format("%s Worker started ...", _workerName));
        } else {
            _log.debug(String.format("%s Worker already running.", _workerName));
            return;
        }
    }

    public  void stop() {
        if (_running.compareAndSet(true, false)) {
            _log.debug(String.format("%s stopping Worker", _workerName));
            _thread.interrupt();
            _log.debug(String.format("%s Worked stopped.", _workerName));
        } else {
            _log.debug(String.format("%s Worker not running.", _workerName));
        }
    }

    public void addToQueue(T element) {
        if (!_running.get()) {
            _log.debug("workers not running, ignoring message");
            return;
        }
        try {
            if (!_running.get()) {
                _log.debug(String.format("%s Worker not running. Can't add items.", _workerName));
                return;
            }

            _queue.add(element);
            _log.debug(String.format("Added to %s queue: %s", _workerName, element.toString()));
        } catch (Exception ex) {
            _log.debug(String.format("Exception on %s Worker addToQueue: %s", _workerName, ex.getMessage()));
        }
    }

    @Override
    public void run() {
        while (_running.get()) {
            try {
                T element = _queue.take();
                _log.debug(String.format("Dequeue: %s", element.toString()));
                executeRefresh(element);
            }  catch (InterruptedException ex) {
                _log.debug("The thread was stopped.");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    protected abstract void executeRefresh(T element);
}
