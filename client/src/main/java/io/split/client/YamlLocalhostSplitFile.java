package io.split.client;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class YamlLocalhostSplitFile extends AbstractLocalhostSplitFile {

    private static final Logger _log = LoggerFactory.getLogger(LegacyLocalhostSplitFile.class);

    public YamlLocalhostSplitFile(LocalhostSplitFactory localhostSplitFactory, String directory, String filenameYaml) throws IOException {
        super(localhostSplitFactory, directory, filenameYaml);
    }

    public Map<SplitAndKey, LocalhostSplit> readOnSplits() throws IOException {
        Map<SplitAndKey, LocalhostSplit> onSplits = Maps.newHashMap();
        try {

            Yaml yaml = new Yaml();
            List<Map<String, Map<String, String>>> yamlSplits = yaml.load(new FileReader(_file));

            for(Map<String, Map<String, String>> aSplit : yamlSplits) {
                // The outter map is a map with one key, the split name
                Map.Entry<String, Map<String, String>> splitAndValues = aSplit.entrySet().iterator().next();

                SplitAndKey splitAndKey = null;
                if (splitAndValues.getValue().get("key") == null) {
                    splitAndKey = SplitAndKey.of(splitAndValues.getKey()); // Key in this line is splitName
                } else {
                    splitAndKey = SplitAndKey.of(splitAndValues.getKey(), splitAndValues.getValue().get("key"));
                }

                onSplits.put(splitAndKey, LocalhostSplit.of(splitAndValues.getValue().get("treatment"), splitAndValues.getValue().get("config")));
            }

        } catch (Exception e) {
            _log.warn("There was no file named " + _file.getPath() + " found. " +
                    "We created a split client that returns default treatments for all features for all of your users. " +
                    "If you wish to return a specific treatment for a feature, enter the name of that feature name and " +
                    "treatment name separated by whitespace in " + _file.getPath() +
                    "; one pair per line. Empty lines or lines starting with '#' are considered comments", e);
        }

        return onSplits;
    }
}
