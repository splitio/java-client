package io.split.client;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.sun.nio.file.SensitivityWatchEventModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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

public class LocalhostSplitFile extends Thread {
    private static final Logger _log = LoggerFactory.getLogger(LocalhostSplitFile.class);

    private final LocalhostSplitFactory _splitFactory;
    private final File _file;
    private final WatchService _watcher;
    private final AtomicBoolean _stop;

    public LocalhostSplitFile(LocalhostSplitFactory splitFactory, String directory, String fileName) throws IOException {
        Preconditions.checkNotNull(directory);
        Preconditions.checkNotNull(fileName);

        _splitFactory = Preconditions.checkNotNull(splitFactory);
        _file = new File(directory, fileName);
        _watcher = FileSystems.getDefault().newWatchService();
        _stop = new AtomicBoolean(false);
    }

    private boolean isStopped() {
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
                    } else if (kind == java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
                            && filename.toString().equals(_file.getName())) {
                        Map<SplitAndKey, String> featureToSplitMap = readOnSplits();
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

    public Map<SplitAndKey, String> readOnSplits() throws IOException {
        Map<SplitAndKey, String> onSplits = Maps.newHashMap();

        try (BufferedReader reader = new BufferedReader(new FileReader(_file))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] feature_treatment = line.split("\\s+");

                if (feature_treatment.length < 2 || feature_treatment.length > 3) {
                    _log.info("Ignoring line since it does not have 2 or 3 columns: " + line);
                    continue;
                }

                SplitAndKey splitAndKey = null;
                if (feature_treatment.length == 2) {
                    splitAndKey = SplitAndKey.of(feature_treatment[0]);
                } else {
                    splitAndKey = SplitAndKey.of(feature_treatment[0], feature_treatment[2]);
                }

                onSplits.put(splitAndKey, feature_treatment[1]);
            }
        } catch (FileNotFoundException e) {
            _log.warn("There was no file named " + _file.getPath() + " found. " +
                    "We created a split client that returns default treatments for all features for all of your users. " +
                    "If you wish to return a specific treatment for a feature, enter the name of that feature name and " +
                    "treatment name separated by whitespace in " + _file.getPath() +
                    "; one pair per line. Empty lines or lines starting with '#' are considered comments", e);
        }

        return onSplits;
    }
}