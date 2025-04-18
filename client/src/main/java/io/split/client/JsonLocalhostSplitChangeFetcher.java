package io.split.client;

import com.google.gson.stream.JsonReader;
import io.split.client.dtos.ChangeDto;
import io.split.client.dtos.SplitChange;
import io.split.client.utils.InputStreamProvider;
import io.split.client.utils.Json;
import io.split.client.utils.LocalhostSanitizer;
import io.split.engine.common.FetchOptions;
import io.split.engine.experiments.SplitChangeFetcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class JsonLocalhostSplitChangeFetcher implements SplitChangeFetcher {

    private static final Logger _log = LoggerFactory.getLogger(JsonLocalhostSplitChangeFetcher.class);
    private final InputStreamProvider _inputStreamProvider;
    private byte [] lastHashFeatureFlags;
    private byte [] lastHashRuleBasedSegments;

    public JsonLocalhostSplitChangeFetcher(InputStreamProvider inputStreamProvider) {
        _inputStreamProvider = inputStreamProvider;
        lastHashFeatureFlags = new byte[0];
        lastHashRuleBasedSegments = new byte[0];
    }

    @Override
    public SplitChange fetch(long since, long sinceRBS, FetchOptions options) {
        try {
            JsonReader jsonReader = new JsonReader(new BufferedReader(new InputStreamReader(_inputStreamProvider.get(), StandardCharsets.UTF_8)));
            SplitChange splitChange = Json.fromJson(jsonReader, SplitChange.class);
            return processSplitChange(splitChange, since, sinceRBS);
        } catch (Exception e) {
            throw new IllegalStateException("Problem fetching splitChanges: " + e.getMessage(), e);
        }
    }

    private SplitChange processSplitChange(SplitChange splitChange, long changeNumber, long changeNumberRBS) throws NoSuchAlgorithmException {
        SplitChange splitChangeToProcess = LocalhostSanitizer.sanitization(splitChange);
        // if the till is less than storage CN and different from the default till ignore the change
        if (checkExitConditions(splitChangeToProcess.featureFlags,  changeNumber) ||
            checkExitConditions(splitChangeToProcess.ruleBasedSegments,  changeNumberRBS)) {
            _log.warn("The till is lower than the change number or different to -1");
            return null;
        }
        byte [] currHashFeatureFlags = getStringDigest(splitChange.featureFlags.d.toString());
        byte [] currHashRuleBasedSegments = getStringDigest(splitChange.ruleBasedSegments.d.toString());
        //if sha exist and is equal to before sha, or if till is equal to default till returns the same segmentChange with till equals to storage CN
        if (Arrays.equals(lastHashFeatureFlags, currHashFeatureFlags) || splitChangeToProcess.featureFlags.t == -1) {
            splitChangeToProcess.featureFlags.t = changeNumber;
        }
        if (Arrays.equals(lastHashRuleBasedSegments, currHashRuleBasedSegments) || splitChangeToProcess.ruleBasedSegments.t == -1) {
            splitChangeToProcess.ruleBasedSegments.t = changeNumberRBS;
        }

        lastHashFeatureFlags = currHashFeatureFlags;
        lastHashRuleBasedSegments = currHashRuleBasedSegments;
        splitChangeToProcess.featureFlags.s = changeNumber;
        splitChangeToProcess.ruleBasedSegments.s = changeNumberRBS;

        return splitChangeToProcess;
    }

    private <T> boolean checkExitConditions(ChangeDto<T> change, long cn) {
        return change.t < cn && change.t != -1;
    }

    private byte[] getStringDigest(String Json) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        digest.update(Json.getBytes());
        // calculate the json sha
        return digest.digest();
    }
}