package io.split.client;

import com.google.gson.stream.JsonReader;
import io.split.client.dtos.SegmentChange;
import io.split.client.utils.Json;
import io.split.engine.common.FetchOptions;
import io.split.engine.segments.SegmentChangeFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class LocalhostSegmentChangeFetcher implements SegmentChangeFetcher {

    private static final Logger _log = LoggerFactory.getLogger(LocalhostSegmentChangeFetcher.class);
    private final File _file;

    public LocalhostSegmentChangeFetcher(String filePath){
        _file = new File(filePath);
    }

    @Override
    public SegmentChange fetch(String segmentName, long changesSinceThisChangeNumber, FetchOptions options) {
        try {
            JsonReader jsonReader = new JsonReader(new FileReader(String.format("%s/%s.json", _file, segmentName)));
            return Json.fromJson(jsonReader, SegmentChange.class);
        }  catch (FileNotFoundException f){
            _log.warn(String.format("There was no file named %s/%s found.", _file.getPath(), segmentName), f);
            throw new IllegalStateException(String.format("Problem fetching segment %s: %s", segmentName, f.getMessage()), f);
        } catch (Exception e) {
            _log.warn(String.format("Problem to fetch segment change for the segment %s in the directory %s.", segmentName, _file.getPath()), e);
            throw new IllegalStateException(String.format("Problem fetching segment %s: %s", segmentName, e.getMessage()), e);
        }
    }
}