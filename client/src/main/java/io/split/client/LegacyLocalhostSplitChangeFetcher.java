package io.split.client;

import io.split.client.dtos.Condition;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import io.split.client.dtos.Status;
import io.split.client.utils.LocalhostConstants;
import io.split.client.utils.LocalhostSanitizer;
import io.split.engine.common.FetchOptions;
import io.split.engine.experiments.SplitChangeFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class LegacyLocalhostSplitChangeFetcher implements SplitChangeFetcher {

    private static final Logger _log = LoggerFactory.getLogger(YamlLocalhostSplitChangeFetcher.class);
    static final String FILENAME = ".split";
    private final File _splitFile;

    public LegacyLocalhostSplitChangeFetcher(String directory) {
        if (directory == null || directory.isEmpty()){
            directory = System.getProperty("user.home");
        }
        _splitFile = new File(directory, FILENAME);
    }

    @Override
    public SplitChange fetch(long since, FetchOptions options) {

        try (BufferedReader reader = new BufferedReader(new FileReader(_splitFile))) {
            SplitChange splitChange = new SplitChange();
            splitChange.splits = new ArrayList<>();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                String lineTrim = line.trim();
                if (lineTrim.isEmpty() || lineTrim.startsWith("#")) {
                    continue;
                }

                String[] featureTreatment = lineTrim.split("\\s+");

                if (featureTreatment.length < 2 || featureTreatment.length > 3) {
                    _log.info("Ignoring line since it does not have 2 or 3 columns: " + lineTrim);
                    continue;
                }
                Optional<Split> splitOptional = splitChange.splits.stream().filter(split -> split.name.equals(featureTreatment[0])).findFirst();
                Split split = splitOptional.orElse(null);
                if(split == null) {
                    split = new Split();
                    split.name = featureTreatment[0];
                    split.configurations = new HashMap<>();
                    split.conditions = new ArrayList<>();
                } else {
                    splitChange.splits.remove(split);
                }
                split.status = Status.ACTIVE;
                split.defaultTreatment = featureTreatment[1];
                split.trafficTypeName = LocalhostConstants.USER;
                split.trafficAllocation = LocalhostConstants.SIZE_100;
                split.trafficAllocationSeed = LocalhostConstants.SIZE_1;

                Condition condition;
                if (featureTreatment.length == 2) {
                    condition = LocalhostSanitizer.createCondition(null, featureTreatment[1]);
                } else {
                    condition = LocalhostSanitizer.createCondition(featureTreatment[2], featureTreatment[1]);
                }
                if(condition.conditionType != ConditionType.ROLLOUT){
                    split.conditions.add(0, condition);
                } else {
                    split.conditions.add(condition);
                }
                splitChange.splits.add(split);
            }
            splitChange.till = since;
            splitChange.since = since;
            return splitChange;
        } catch (FileNotFoundException f) {
            _log.warn("There was no file named " + _splitFile.getPath() + " found. " +
                    "We created a split client that returns default treatments for all feature flags for all of your users. " +
                    "If you wish to return a specific treatment for a feature flag, enter the name of that feature flag name and " +
                    "treatment name separated by whitespace in " + _splitFile.getPath() +
                    "; one pair per line. Empty lines or lines starting with '#' are considered comments", f);
            throw new IllegalStateException("Problem fetching splitChanges: " + f.getMessage(), f);
        } catch (Exception e) {
            _log.warn(String.format("Problem to fetch split change using the file %s",
                    _splitFile.getPath()), e);
            throw new IllegalStateException("Problem fetching splitChanges: " + e.getMessage(), e);
        }
    }
}