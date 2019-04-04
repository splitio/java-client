package io.split.client;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

public class LegacyLocalhostSplitFile extends AbstractLocalhostSplitFile {
    private static final Logger _log = LoggerFactory.getLogger(LegacyLocalhostSplitFile.class);

    public LegacyLocalhostSplitFile(LocalhostSplitFactory splitFactory, String directory, String fileName) throws IOException {
        super(splitFactory, directory, fileName);
    }


    public Map<SplitAndKey, LocalhostSplit> readOnSplits() throws IOException {
        Map<SplitAndKey, LocalhostSplit> onSplits = Maps.newHashMap();

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

                onSplits.put(splitAndKey, new LocalhostSplit(feature_treatment[1], null));
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