package io.split.client;

import io.split.client.dtos.Condition;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import io.split.client.dtos.Status;
import io.split.client.utils.LocalhostConstants;
import io.split.engine.common.FetchOptions;
import io.split.engine.experiments.SplitChangeFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.split.client.utils.LocalhostSanitizer.createCondition;


public class YamlLocalhostSplitChangeFetcher implements SplitChangeFetcher {

    private static final Logger _log = LoggerFactory.getLogger(YamlLocalhostSplitChangeFetcher.class);
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

                Optional<Split> splitOptional = splitChange.splits.stream().filter(split -> split.name.equals(splitAndValues.getKey())).findFirst();
                Split split = splitOptional.orElse(null);
                if(split == null) {
                    split = new Split();
                    split.name = splitAndValues.getKey();
                    split.configurations = new HashMap<>();
                    split.conditions = new ArrayList<>();
                } else {
                    splitChange.splits.remove(split);
                }
                String treatment = (String) splitAndValues.getValue().get("treatment");
                String configurations = splitAndValues.getValue().get("config") != null ? (String) splitAndValues.getValue().get("config") : null;
                Object keyOrKeys = splitAndValues.getValue().get("keys");
                split.configurations.put(treatment, configurations);

                Condition condition = createCondition(keyOrKeys, treatment);
                if(condition.conditionType != ConditionType.ROLLOUT){
                    split.conditions.add(0, condition);
                } else {
                    split.conditions.add(condition);
                }
                split.status = Status.ACTIVE;
                split.defaultTreatment = LocalhostConstants.CONTROL;
                split.trafficTypeName = LocalhostConstants.USER;
                split.trafficAllocation = LocalhostConstants.SIZE_100;
                split.trafficAllocationSeed = LocalhostConstants.SIZE_1;

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
}