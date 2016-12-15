package io.split.client;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * For environment 'localhost', this class reads the list of (feature, treatment) pairs
 * from a file $HOME/.splits, and returns an instance of HardcodedFeatureTreatmentsSplitClient.
 * <p/>
 * <p/>
 * $HOME/.splits has two columns separated by whitespace. First column is the name of the feature,
 * the second column is the name of the treatment to always return for the corresponding feature.
 * Empty lines or lines without exactly two columns are ignored. Lines that start with '#' will be
 * ignored as comments.
 * <p/>
 * <p/>
 * If $HOME/.splits is not found, all features are assumed off.
 *
 * @author adil
 */
public class LocalhostSplitFactoryBuilder {

    private static final Logger _log = LoggerFactory.getLogger(LocalhostSplitFactoryBuilder.class);

    public static final String LOCALHOST = "localhost";

    public static LocalhostSplitFactory build() throws IOException {
        String home = System.getProperty("user.home");
        checkNotNull(home, "Property user.home should be set when using environment: " + LOCALHOST);

        return build(home);
    }

    static LocalhostSplitFactory build(String home) throws IOException {
        return new LocalhostSplitFactory(home);
    }
}
