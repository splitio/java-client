package io.split.client;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
public class LocalhostSplitClientBuilder {

    private static final Logger _log = LoggerFactory.getLogger(LocalhostSplitClientBuilder.class);

    public static final String LOCALHOST = "localhost";
    public static final String FILENAME = ".split";

    public static LocalhostSplitClient build() throws IOException {
        String home = System.getProperty("user.home");
        checkNotNull(home, "Property user.home should be set when using environment: " + LOCALHOST);

        return build(home);
    }

    static LocalhostSplitClient build(String home) throws IOException {
        _log.info("home = " + home);

        BufferedReader reader = null;

        String fileName = home + "/" + FILENAME;

        Map<String, String> onSplits = Maps.newHashMap();

        try {
            reader = new BufferedReader(new FileReader(new File(fileName)));

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] feature_treatment = line.split("\\s+");

                if (feature_treatment.length != 2) {
                    _log.info("Ignoring line since it does not have exactly two columns: " + line);
                    continue;
                }

                onSplits.put(feature_treatment[0], feature_treatment[1]);
                _log.info("100% of keys will see " + feature_treatment[1] + " for " + feature_treatment[0]);

            }
        } catch (FileNotFoundException e) {
            _log.warn("There was no file named " + fileName + " found. " +
                    "We created a split client that returns default treatments for all features for all of your users. " +
                    "If you wish to return a specific treatment for a feature, enter the name of that feature name and " +
                    "treatment name separated by whitespace in " + fileName +
                    "; one pair per line. Empty lines or lines starting with '#' are considered comments", e);
        } catch (IOException e) {
            throw e;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        return new LocalhostSplitClient(onSplits);
    }

}
