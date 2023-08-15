package io.split.client;

import com.google.gson.stream.JsonReader;
import io.split.client.dtos.SplitChange;
import io.split.client.utils.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Map;

public class JsonFileLocalhostSplitChangeFetcher extends JsonLocalhostSplitChangeFetcher {

    private static final Logger _log = LoggerFactory.getLogger(JsonFileLocalhostSplitChangeFetcher.class);
    private final File _file;


    public JsonFileLocalhostSplitChangeFetcher(String filePath) {
        _file = new File(filePath);
        super.lastHash = new byte[0];
    }

    @Override
    public JsonReader readFile() {
        try {
            return new JsonReader(new FileReader("_file"));
        } catch (FileNotFoundException f) {
            _log.warn(String.format("There was no file named %s found. " +
                            "We created a split client that returns default treatments for all feature flags for all of your users. " +
                            "If you wish to return a specific treatment for a feature flag, enter the name of that feature flag name and " +
                            "treatment name separated by whitespace in %s; one pair per line. Empty lines or lines starting with '#' are " +
                            "considered comments",
                    getFilePath(), getFilePath()), f);
            throw new IllegalStateException("Problem fetching splitChanges: " + f.getMessage(), f);
        }
    }

    @Override
    public String getFilePath() {
        return _file.getPath();
    }
}