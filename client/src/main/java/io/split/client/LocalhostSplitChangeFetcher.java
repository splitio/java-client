package io.split.client;

import com.google.gson.stream.JsonReader;
import io.split.client.dtos.SplitChange;
import io.split.client.utils.Json;
import io.split.client.utils.LocalhostSanitizer;
import io.split.engine.common.FetchOptions;
import io.split.engine.experiments.SplitChangeFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class LocalhostSplitChangeFetcher implements SplitChangeFetcher {

    private static final Logger _log = LoggerFactory.getLogger(LocalhostSplitChangeFetcher.class);
    private final File _file;
    private byte [] lastHash;

    public LocalhostSplitChangeFetcher(String filePath) {
        _file = new File(filePath);
        lastHash = new byte[0];
    }

    @Override
    public SplitChange fetch(long since, FetchOptions options) {

        try {
            JsonReader jsonReader = new JsonReader(new FileReader(_file));
            SplitChange splitChange = Json.fromJson(jsonReader, SplitChange.class);
            return processSplitChange(splitChange, since);
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

    private SplitChange processSplitChange(SplitChange splitChange, long changeNumber) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        SplitChange splitChangeToProcess = LocalhostSanitizer.sanitization(splitChange);
        if (splitChangeToProcess.till < changeNumber && splitChange.till != -1) {
            _log.warn("The till is lower than the change number or different to -1");
            return null;
        }
        String splitJson = splitChange.splits.toString();
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        digest.update(splitJson.getBytes());
        byte [] currHash = digest.digest();
        if (Arrays.equals(lastHash, currHash) || splitChangeToProcess.till == -1) {
            lastHash = currHash;
            splitChangeToProcess.since = changeNumber;
            splitChangeToProcess.till = changeNumber;
            return splitChangeToProcess;
        }
        lastHash = currHash;
        splitChangeToProcess.since = changeNumber;
        return splitChangeToProcess;
    }
}