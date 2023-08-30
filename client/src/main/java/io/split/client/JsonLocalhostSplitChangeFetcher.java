package io.split.client;

import com.google.gson.stream.JsonReader;
import io.split.client.dtos.SplitChange;
import io.split.client.exceptions.InputStreamProviderException;
import io.split.client.utils.InputStreamProvider;
import io.split.client.utils.Json;
import io.split.client.utils.LocalhostSanitizer;
import io.split.engine.common.FetchOptions;
import io.split.engine.experiments.SplitChangeFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class JsonLocalhostSplitChangeFetcher implements SplitChangeFetcher {

    private static final Logger _log = LoggerFactory.getLogger(JsonLocalhostSplitChangeFetcher.class);
    private final InputStreamProvider _inputStreamProvider;
    private byte [] lastHash;

    public JsonLocalhostSplitChangeFetcher(InputStreamProvider inputStreamProvider) {
        _inputStreamProvider = inputStreamProvider;
        lastHash = new byte[0];
    }

    @Override
    public SplitChange fetch(long since, FetchOptions options) {
        try {
            JsonReader jsonReader = new JsonReader(new BufferedReader(new InputStreamReader(_inputStreamProvider.get(), "UTF-8")));
            SplitChange splitChange = Json.fromJson(jsonReader, SplitChange.class);
            return processSplitChange(splitChange, since);
        } catch (InputStreamProviderException i) {
            _log.warn(String.format("Problem to fetch split change using file named %s", i.getFileName()));
            throw new IllegalStateException("Problem fetching splitChanges: " + i.getMessage(), i);
        } catch (Exception e) {
            _log.warn(String.format("Problem to fetch split change using a file"), e);
            throw new IllegalStateException("Problem fetching splitChanges: " + e.getMessage(), e);
        }
    }

    private SplitChange processSplitChange(SplitChange splitChange, long changeNumber) throws NoSuchAlgorithmException {
        SplitChange splitChangeToProcess = LocalhostSanitizer.sanitization(splitChange);
        // if the till is less than storage CN and different from the default till ignore the change
        if (splitChangeToProcess.till < changeNumber && splitChangeToProcess.till != -1) {
            _log.warn("The till is lower than the change number or different to -1");
            return null;
        }
        String splitJson = splitChange.splits.toString();
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        digest.update(splitJson.getBytes());
        // calculate the json sha
        byte [] currHash = digest.digest();
        //if sha exist and is equal to before sha, or if till is equal to default till returns the same segmentChange with till equals to storage CN
        if (Arrays.equals(lastHash, currHash) || splitChangeToProcess.till == -1) {
            splitChangeToProcess.till = changeNumber;
        }
        lastHash = currHash;
        splitChangeToProcess.since = changeNumber;
        return splitChangeToProcess;
    }
}