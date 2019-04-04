package io.split.client;

import com.google.common.base.Preconditions;
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
 * @author adil
 */
public final class LocalhostSplitFactory implements SplitFactory {
    private static final Logger _log = LoggerFactory.getLogger(LocalhostSplitFactory.class);

    static final String FILENAME = ".split";
    static final String FILENAME_YAML = ".split.yaml";
    static final String LOCALHOST = "localhost";

    private final LocalhostSplitClientAndFactory _client;
    private final LocalhostSplitManager _manager;
    private final AbstractLocalhostSplitFile _splitFile;

    public static LocalhostSplitFactory createLocalhostSplitFactory() throws IOException {
        String directory = System.getProperty("user.home");
        Preconditions.checkNotNull(directory, "Property user.home should be set when using environment: " + LOCALHOST);
        return new LocalhostSplitFactory(directory);
    }

    /**
     * Visible for testing
     */
    public LocalhostSplitFactory(String directory, String file) throws IOException {
        //Preconditions.checkNotNull(directory, "directory must not be null");

        _log.info("home = " + directory);

        File yaml = null;
        if (directory == null) {
            directory = "";
        }
        yaml = new File(directory, file);

        if (yaml.exists()) {
            _splitFile = new YamlLocalhostSplitFile(this, directory, file);
        } else {
            _splitFile = new LegacyLocalhostSplitFile(this, directory, FILENAME);
        }

        Map<SplitAndKey, LocalhostSplit> splitAndKeyToTreatment = _splitFile.readOnSplits();
        _client = new LocalhostSplitClientAndFactory(this, new LocalhostSplitClient(splitAndKeyToTreatment));
        _manager = LocalhostSplitManager.of(splitAndKeyToTreatment);

        _splitFile.registerWatcher();
        _splitFile.setDaemon(true);
        _splitFile.start();
    }

    public LocalhostSplitFactory(String directory) throws IOException {
        this(directory, FILENAME_YAML);
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
