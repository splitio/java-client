package io.split.client;

import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import io.split.client.dtos.SegmentChange;
import io.split.client.utils.Json;
import io.split.client.utils.LocalhostSanitizer;
import io.split.engine.common.FetchOptions;
import io.split.engine.segments.SegmentChangeFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LocalhostSegmentChangeFetcher implements SegmentChangeFetcher {

    private static final Logger _log = LoggerFactory.getLogger(LocalhostSegmentChangeFetcher.class);
    private final File _file;

    private Map lastHash;

    public LocalhostSegmentChangeFetcher(String filePath){
        _file = new File(filePath);
        lastHash = new HashMap<String, byte []>();
    }

    @Override
    public SegmentChange fetch(String segmentName, long changesSinceThisChangeNumber, FetchOptions options) {
        try {
            JsonReader jsonReader = new JsonReader(new FileReader(String.format("%s/%s.json", _file, segmentName)));
            SegmentChange segmentChange = Json.fromJson(jsonReader, SegmentChange.class);
            return processSegmentChange(segmentName, changesSinceThisChangeNumber, segmentChange);
        }  catch (FileNotFoundException f){
            _log.warn(String.format("There was no file named %s/%s found.", _file.getPath(), segmentName), f);
            throw new IllegalStateException(String.format("Problem fetching segment %s: %s", segmentName, f.getMessage()), f);
        } catch (Exception e) {
            _log.warn(String.format("Problem to fetch segment change for the segment %s in the directory %s.", segmentName, _file.getPath()), e);
            throw new IllegalStateException(String.format("Problem fetching segment %s: %s", segmentName, e.getMessage()), e);
        }
    }

    private SegmentChange processSegmentChange(String segmentName, long changeNumber, SegmentChange segmentChange) throws NoSuchAlgorithmException {
        SegmentChange segmentChangeToProcess = LocalhostSanitizer.sanitization(segmentChange);
        if (segmentChangeToProcess.till < changeNumber && segmentChange.till != -1){
            _log.warn("The segmentChange till is lower than the change number or different to -1");
            return null;
        }
        String toHash = segmentChangeToProcess.added.toString() + segmentChangeToProcess.removed.toString();
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        digest.update(toHash.getBytes());
        byte [] currHash = digest.digest();
        if ((lastHash.containsKey(segmentName) && Arrays.equals((byte[]) lastHash.get(segmentName), currHash)) ||
            segmentChangeToProcess.till == -1) {
            lastHash.put(segmentName, currHash);
            segmentChangeToProcess.since = changeNumber;
            segmentChangeToProcess.till = changeNumber;
            return segmentChangeToProcess;
        }
        lastHash.put(segmentName, currHash);
        segmentChangeToProcess.since = changeNumber;
        return segmentChangeToProcess;
    }
}