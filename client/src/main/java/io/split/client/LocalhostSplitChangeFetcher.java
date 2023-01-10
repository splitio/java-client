package io.split.client;

import com.google.gson.stream.JsonReader;
import io.split.client.dtos.SplitChange;
import io.split.client.utils.Json;
import io.split.engine.common.FetchOptions;
import io.split.engine.experiments.SplitChangeFetcher;
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
        try {
            JsonReader jsonReader = new JsonReader(new FileReader(_file));
            return Json.fromJson(jsonReader, SplitChange.class);
        } catch (Exception e) {
            _log.warn(String.format("Problem to fetch split change using the file %s",
                    _file.getPath()), e);
            throw new IllegalStateException("Problem fetching splitChanges: " + e.getMessage(), e);
        }
    }
}