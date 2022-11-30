package io.split.engine.experiments;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import io.split.client.dtos.SplitChange;
import io.split.engine.common.FetchOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;

public class LocalhostSplitChangeFetcher implements SplitChangeFetcher {

    private static final Logger _log = LoggerFactory.getLogger(LocalhostSplitChangeFetcher.class);
    private final File _file;

    public LocalhostSplitChangeFetcher(String filePath){
        _file = new File(filePath);
    }

    @Override
    public SplitChange fetch(long since, FetchOptions options) {
        SplitChange splitChange = null;
        try {
            JsonReader jsonReader = new JsonReader(new FileReader(_file));

            Gson gson = new Gson();
            splitChange = gson.fromJson(jsonReader, SplitChange.class);
        } catch (Exception e) {
            _log.warn("There was no file named " + _file.getPath() + " found. " +
                    "We created a split client that returns default treatments for all features for all of your users. " +
                    "If you wish to return a specific treatment for a feature, enter the name of that feature name and " +
                    "treatment name separated by whitespace in " + _file.getPath() +
                    "; one pair per line. Empty lines or lines starting with '#' are considered comments", e);
        }
        return splitChange;
    }
}
