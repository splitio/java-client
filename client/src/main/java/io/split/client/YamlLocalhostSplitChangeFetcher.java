package io.split.client;

import com.google.common.base.Preconditions;
import io.split.client.dtos.Condition;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.KeySelector;
import io.split.client.dtos.Matcher;
import io.split.client.dtos.MatcherCombiner;
import io.split.client.dtos.MatcherGroup;
import io.split.client.dtos.MatcherType;
import io.split.client.dtos.Partition;
import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import io.split.client.dtos.Status;
import io.split.client.dtos.WhitelistMatcherData;
import io.split.client.utils.LocalhostSanitizer;
import io.split.engine.common.FetchOptions;
import io.split.engine.experiments.SplitChangeFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YamlLocalhostSplitChangeFetcher implements SplitChangeFetcher {

    private static final Logger _log = LoggerFactory.getLogger(JsonLocalhostSplitChangeFetcher.class);
    static final String FILENAME = ".split";
    private final File _splitFile;

    public YamlLocalhostSplitChangeFetcher(String filePath) {
        _splitFile = new File(filePath);
    }

    @Override
    public SplitChange fetch(long since, FetchOptions options) {
        try {
            Yaml yaml = new Yaml();
            List<Map<String, Map<String, Object>>> yamlSplits = yaml.load(new FileReader(_splitFile));
            SplitChange splitChange = new SplitChange();
            splitChange.splits = new ArrayList<>();
            for(Map<String, Map<String, Object>> aSplit : yamlSplits) {
                // The outter map is a map with one key, the split name
                Map.Entry<String, Map<String, Object>> splitAndValues = aSplit.entrySet().iterator().next();

                Split split = new Split();

                String splitName = splitAndValues.getKey();
                String treatment = (String) splitAndValues.getValue().get("treatment");
                String configurations = splitAndValues.getValue().get("config") != null ? (String) splitAndValues.getValue().get("config") : null;
                Object keyOrKeys = splitAndValues.getValue().get("keys");

                split.name = splitName;
                Map<String, String> configMap = new HashMap<>();
                configMap.put(treatment, configurations);
                split.configurations = configMap;
                Condition condition = createCondition(keyOrKeys, treatment);
                split.conditions = new ArrayList<>();
                split.conditions.add(condition);
                split.status = Status.ACTIVE;
                split.defaultTreatment = "on";
                split.trafficTypeName = "user";
                split.trafficAllocation = 100;
                split.trafficAllocationSeed = 1;

                splitChange.splits.add(split);
            }
            splitChange.till = since;
            splitChange.since = since;
            return splitChange;
        } catch (FileNotFoundException f) {
            _log.warn(String.format("There was no file named %s found. " +
                            "We created a split client that returns default treatments for all features for all of your users. " +
                            "If you wish to return a specific treatment for a feature, enter the name of that feature name and " +
                            "treatment name separated by whitespace in %s; one pair per line. Empty lines or lines starting with '#' are considered comments",
                    _splitFile.getPath(), _splitFile.getPath()), f);
            throw new IllegalStateException("Problem fetching splitChanges: " + f.getMessage(), f);
        } catch (Exception e) {
            _log.warn(String.format("Problem to fetch split change using the file %s",
                    _splitFile.getPath()), e);
            throw new IllegalStateException("Problem fetching splitChanges: " + e.getMessage(), e);
        }
    }

    private Condition createCondition(Object keyOrKeys, String treatment) {
        Condition condition = new Condition();
        if (keyOrKeys == null) {
            return LocalhostSanitizer.createRolloutCondition(condition, "user", treatment);
        } else {
            if (keyOrKeys instanceof String) {
                List keys = new ArrayList<>();
                keys.add(keyOrKeys);
                return createWhitelistCondition(condition, "user", treatment, keys);
            } else {
                Preconditions.checkArgument(keyOrKeys instanceof List, "'keys' is not a String nor a List.");
                return createWhitelistCondition(condition, "user", treatment, (List<String>) keyOrKeys);
            }
        }
    }

    private Condition createWhitelistCondition(Condition condition, String trafficType, String treatment, List<String> keys) {
        condition.conditionType = ConditionType.WHITELIST;
        condition.matcherGroup = new MatcherGroup();
        condition.matcherGroup.combiner = MatcherCombiner.AND;
        Matcher matcher = new Matcher();
        KeySelector keySelector = new KeySelector();
        keySelector.trafficType = trafficType;

        matcher.keySelector = keySelector;
        matcher.matcherType = MatcherType.WHITELIST;
        matcher.negate = false;
        matcher.whitelistMatcherData = new WhitelistMatcherData();
        matcher.whitelistMatcherData.whitelist = new ArrayList<>(keys);

        condition.matcherGroup.matchers = new ArrayList<>();
        condition.matcherGroup.matchers.add(matcher);

        condition.partitions = new ArrayList<>();
        Partition partition1 = new Partition();
        Partition partition2 = new Partition();
        partition1.size = 100;
        partition2.size = 0;
        if (treatment != null) {
            partition1.treatment = treatment;
        } else {
            partition1.treatment = "off";
            partition2.treatment = "on";
        }
        condition.partitions.add(partition1);
        condition.partitions.add(partition2);
        condition.label = "default rule";

        return condition;
    }
}
