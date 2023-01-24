package io.split.client;

import com.google.gson.stream.JsonReader;
import io.split.client.dtos.Condition;
import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import io.split.client.dtos.Status;
import io.split.client.utils.Json;
import io.split.engine.common.FetchOptions;
import io.split.engine.experiments.SplitChangeFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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
            ObjectMapper objectMapper = new ObjectMapper();
            SplitChange splitChange = objectMapper.readValue(new FileReader(_file), SplitChange.class);
            System.out.println(splitChange.splits != null);
//            return Json.fromJson(jsonReader, SplitChange.class);
            return sanitization(splitChange);
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

    public SplitChange sanitization(SplitChange splitChange) {
        Random random = new Random();
        if (splitChange.till == null || splitChange.till < -1) {
            splitChange.till = new Long("-1");
        }
        if (splitChange.since == null || splitChange.since < -1 || splitChange.since > splitChange.till) {
            splitChange.since = new Long(splitChange.till.longValue());
        }
        if (splitChange.splits != null) {
            for (Split split: splitChange.splits) {
                if (split.name == null){
                    splitChange.splits.remove(split);
                    continue;
                }
                if (split.trafficTypeName == null || split.trafficTypeName.isEmpty()) {
                    split.trafficTypeName = "user";
                }
                if (split.trafficAllocation == null || split.trafficAllocation < 0 || split.trafficAllocation > 100) {
                    split.trafficAllocation = 100;
                }
                if (split.trafficAllocationSeed == null) {
                    split.trafficAllocationSeed = random.nextInt(10) * 1000;
                }
                if (split.seed == null) {
                    split.seed = random.nextInt(10) * 1000;
                }
                if (split.status != Status.ACTIVE || split.status != Status.ARCHIVED) {
                    split.status = Status.ACTIVE;
                }
                if (split.killed == null) {
                    split.killed = false;
                }
                if (split.defaultTreatment == null || split.defaultTreatment.isEmpty()) {
                    split.defaultTreatment = "on";
                }
                if (split.changeNumber == null || split.changeNumber < 0) {
                    split.changeNumber = new Long(0);
                }
                if (split.algo == null || split.algo != 2){
                    split.algo = 2;
                }
                if (split.conditions == null) {
                    split.conditions = new ArrayList<>();
                }

                List<Condition> hasAllKeys = split.conditions.stream()
                        .filter(condition -> !condition.matcherGroup.matchers.stream().filter(
                                matcher -> matcher.matcherType.equals("ALL_KEYS")).
                                collect(Collectors.toList()).isEmpty())
                        .collect(Collectors.toList());
                if (hasAllKeys.isEmpty()) {
                    try {
                        JsonReader jsonReader = new JsonReader(new FileReader("src/main/resources/condition.json"));
                        Condition condition = Json.fromJson(jsonReader, Condition.class);
                        split.conditions.add(condition);
                    } catch (Exception e) {
                        _log.warn(String.format("Problem adding condition"));
                    }
                }
            }
            return splitChange;
        }
        System.out.println(splitChange.till);
        splitChange.splits = new ArrayList<>();
        return splitChange;
    }
}