package io.split.client;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * An implementation of SplitClient that considers all partitions
 * passed in the constructor to be 100% on for all users, and
 * any other split to be 100% off for all users. This implementation
 * is useful for using Codigo in localhost environment.
 *
 * @author adil
 */
public final class LocalhostSplitFactory implements SplitFactory {
    private static final Logger _log = LoggerFactory.getLogger(LocalhostSplitFactory.class);

    static final String FILENAME = ".split";
    static final String LOCALHOST = "localhost";

    private final LocalhostSplitClient _client;
    private final LocalhostSplitManager _manager;
    private final LocalhostSplitFile _splitFile;

    public static LocalhostSplitFactory createLocalhostSplitFactory() throws IOException {
        String directory = System.getProperty("user.home");
        Preconditions.checkNotNull(directory, "Property user.home should be set when using environment: " + LOCALHOST);
        return new LocalhostSplitFactory(directory);
    }

    public LocalhostSplitFactory(String directory) throws IOException {
        Preconditions.checkNotNull(directory, "directory must not be null");

        _log.info("home = " + directory);

        _splitFile = new LocalhostSplitFile(this, directory, FILENAME);

        Map<String, String> _featureToTreatmentMap = _splitFile.readOnSplits();
        _client = new LocalhostSplitClient(_featureToTreatmentMap);
        _manager = new LocalhostSplitManager(_featureToTreatmentMap);

        _splitFile.registerWatcher();
        _splitFile.start();
    }

    @Override
    public SplitClient client() {
        return _client;
    }

    @Override
    public SplitManager manager() {
        return _manager;
    }

    public void updateFeatureToTreatmentMap(Map<String, String> featureToTreatmentMap) {
        _client.updateFeatureToTreatmentMap(featureToTreatmentMap);
        _manager.updateFeatureToTreatmentMap(featureToTreatmentMap);
    }
}
