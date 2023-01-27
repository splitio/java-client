package io.split.client;

import io.split.client.dtos.SplitChange;
import io.split.client.utils.LocalhostSanitizer;
import io.split.engine.common.FetchOptions;
import io.split.engine.experiments.SplitChangeFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class LocalhostSplitChangeFetcher implements SplitChangeFetcher {

    private static final Logger _log = LoggerFactory.getLogger(LocalhostSplitChangeFetcher.class);
    private final File _file;

    public LocalhostSplitChangeFetcher(String filePath){
        _file = new File(filePath);
    }

    @Override
    public SplitChange fetch(long since, FetchOptions options) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            SplitChange splitChange = objectMapper.readValue(new FileReader(_file), SplitChange.class);
            return LocalhostSanitizer.sanitization(splitChange);
        } catch (FileNotFoundException f){
            _log.warn(String.format("There was no file named %s found. " +
                            "We created a split client that returns default treatments for all features for all of your users. " +
                            "If you wish to return a specific treatment for a feature, enter the name of that feature name and " +
                            "treatment name separated by whitespace in %s; one pair per line. Empty lines or lines starting with '#' are considered comments",
                    _file.getPath(), _file.getPath()), f);
            throw new IllegalStateException("Problem fetching splitChanges: " + f.getMessage(), f);
        } catch (Exception e) {
            _log.warn(String.format("Problem to fetch split change using the file %s",
                    _file.getPath()), e);
            throw new IllegalStateException("Problem fetching splitChanges: " + e.getMessage(), e);
        }
    }
}