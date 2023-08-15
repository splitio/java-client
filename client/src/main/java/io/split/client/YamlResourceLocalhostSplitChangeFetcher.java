package io.split.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class YamlResourceLocalhostSplitChangeFetcher extends YamlLocalhostSplitChangeFetcher {
    private static final Logger _log = LoggerFactory.getLogger(YamlFileLocalhostSplitChangeFetcher.class);
    private final String _fileName;

    public YamlResourceLocalhostSplitChangeFetcher(String fileName) {
        _fileName = fileName;
    }
    @Override
    public List<Map<String, Map<String, Object>>> readFile() {
        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(_fileName);
            return yaml.load(inputStream);
        } catch (Exception e) {
            _log.warn(String.format("Problem to fetch split change using the file %s",
                    _fileName), e);
            throw new IllegalStateException("Problem fetching splitChanges: " + e.getMessage(), e);
        }
    }

    @Override
    public String getFilePath() {
        return _fileName;
    }
}