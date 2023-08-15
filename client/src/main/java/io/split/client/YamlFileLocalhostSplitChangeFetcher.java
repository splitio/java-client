package io.split.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Map;

public class YamlFileLocalhostSplitChangeFetcher extends YamlLocalhostSplitChangeFetcher{

    private static final Logger _log = LoggerFactory.getLogger(YamlFileLocalhostSplitChangeFetcher.class);
    private final File _splitFile;

    public YamlFileLocalhostSplitChangeFetcher(String fileName) {
        _splitFile = new File(fileName);
    }

    @Override
    public List<Map<String, Map<String, Object>>> readFile() {
        try {
            Yaml yaml = new Yaml();
            return yaml.load(new FileReader(_splitFile));
        } catch (FileNotFoundException f) {
            _log.warn(String.format("There was no file named %s found. We created a split client that returns default treatments " +
                            "for all feature flags for all of your users. If you wish to return a specific treatment for a feature flag, " +
                            "enter the name of that feature flag name and treatment name separated by whitespace in %s; one pair per line. " +
                            "Empty lines or lines starting with '#' are considered comments",
                    _splitFile.getPath(), _splitFile.getPath()), f);
            throw new IllegalStateException("Problem fetching splitChanges: " + f.getMessage(), f);
        } catch (Exception e) {
            _log.warn(String.format("Problem to fetch split change using the file %s",
                    _splitFile.getPath()), e);
            throw new IllegalStateException("Problem fetching splitChanges: " + e.getMessage(), e);
        }
    }

    @Override
    public String getFilePath() {
        return _splitFile.getPath();
    }
}