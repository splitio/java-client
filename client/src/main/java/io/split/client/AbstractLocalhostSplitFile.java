package io.split.client;

import com.google.common.base.Preconditions;
import com.sun.nio.file.SensitivityWatchEventModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractLocalhostSplitFile extends Thread {
    private static final Logger _log = LoggerFactory.getLogger(AbstractLocalhostSplitFile.class);

    protected final LocalhostSplitFactory _splitFactory;
    protected final File _file;
    protected final WatchService _watcher;
    protected final AtomicBoolean _stop;

    public AbstractLocalhostSplitFile(LocalhostSplitFactory splitFactory, String directory, String fileName) throws IOException {
        Preconditions.checkNotNull(directory);
        Preconditions.checkNotNull(fileName);

        _splitFactory = Preconditions.checkNotNull(splitFactory);

        //  If no directory is set, instantiate the file without parent, otherwise the path separator is inserted
        // before the filename in the java.io.File.File(java.lang.String, java.lang.String) class (see line 319).
        _file = (directory.length() > 0) ?
                new File(directory, fileName) :
                new File(fileName);

        _watcher = FileSystems.getDefault().newWatchService();
        _stop = new AtomicBoolean(false);
    }

    public boolean isStopped() {
        return _stop.get();
    }

    public void stopThread() {
        _stop.set(true);
    }

    public void registerWatcher() throws IOException {
        Path path = _file.toPath().toAbsolutePath().getParent();
        path.register(_watcher, new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_MODIFY}, SensitivityWatchEventModifier.HIGH);
    }

    @Override
    public void run() {
        try {
            while (!isStopped()) {
                WatchKey key;
                try {
                    key = _watcher.poll(250, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    stopThread();
                    return;
                }
                if (key == null) {
                    Thread.yield();
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        Thread.yield();
                        continue;
                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY
                            && filename.toString().equals(_file.getName())) {
                        Map<SplitAndKey, LocalhostSplit> featureToSplitMap = readOnSplits();
                        _splitFactory.updateFeatureToTreatmentMap(featureToSplitMap);
                        _log.info("Detected change in Local Splits file - Splits Reloaded! file={}", _file.getPath());
                    }
                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }
                Thread.yield();
            }
        } catch (IOException e) {
            _log.error("Error reading file: path={}", _file.getPath(), e);
            stopThread();
        }
    }

    public abstract Map<SplitAndKey, LocalhostSplit> readOnSplits() throws IOException;

}