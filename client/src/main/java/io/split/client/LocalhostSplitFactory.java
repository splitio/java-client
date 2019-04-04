package io.split.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * An implementation of SplitClient that considers all partitions
 * passed in the constructor to be 100% on for all users, and
 * any other split to be 100% off for all users. This implementation
 * is useful for using Split in localhost environment.
 *
 * The startup order is as follows:
 * 1. Split will look for $user.home/.split file if it exists (for backward compatibility with older versions)
 * 2. Split will use config.splitFile (full path)
 * 3. If full path is not specified, then $user.home/config.splitFile
 *
 */
public final class LocalhostSplitFactory implements SplitFactory {
    private static final Logger _log = LoggerFactory.getLogger(LocalhostSplitFactory.class);

    static final String FILENAME = ".split";
    static final String LOCALHOST = "localhost";

    private final LocalhostSplitClientAndFactory _client;
    private final LocalhostSplitManager _manager;
    private final AbstractLocalhostSplitFile _splitFile;

    public static LocalhostSplitFactory createLocalhostSplitFactory(SplitClientConfig config) throws IOException {
        String directory = System.getProperty("user.home");
        return new LocalhostSplitFactory(directory, config.splitFile());
    }

    public LocalhostSplitFactory(String directory, String file) throws IOException {

        if (directory == null || file.contains("/")) {
            directory = "";
        }

        File legacyFile = new File(directory, ".split");
        if (legacyFile.exists()) {
            _splitFile = new LegacyLocalhostSplitFile(this, directory, FILENAME);
            _log.warn("(Deprecated) Starting Split in localhost mode using legacy file located at " + _splitFile._file.getAbsolutePath()
                    + "\nPlease delete this file or split.yaml location will be ignored");
        } else {
            _splitFile = new YamlLocalhostSplitFile(this, directory, file);
            _log.info("Starting Split in localhost mode with file at " + _splitFile._file.getAbsolutePath());
        }


        Map<SplitAndKey, LocalhostSplit> splitAndKeyToTreatment = _splitFile.readOnSplits();
        _client = new LocalhostSplitClientAndFactory(this, new LocalhostSplitClient(splitAndKeyToTreatment));
        _manager = LocalhostSplitManager.of(splitAndKeyToTreatment);

        _splitFile.registerWatcher();
        _splitFile.setDaemon(true);
        _splitFile.start();
    }

    public LocalhostSplitFactory(String directory) throws IOException {
        this(directory, SplitClientConfig.LOCALHOST_DEFAULT_FILE);
    }

    @Override
    public SplitClient client() {
        return _client;
    }

    @Override
    public SplitManager manager() {
        return _manager;
    }

    @Override
    public void destroy() {
        _splitFile.stopThread();
    }

    @Override
    public boolean isDestroyed() {
        return _splitFile.isStopped();
    }

    public void updateFeatureToTreatmentMap(Map<SplitAndKey, LocalhostSplit> featureToTreatmentMap) {
        _client.updateFeatureToTreatmentMap(featureToTreatmentMap);
        _manager.updateFeatureToTreatmentMap(featureToTreatmentMap);
    }
}
