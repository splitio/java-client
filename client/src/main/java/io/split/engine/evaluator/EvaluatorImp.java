package io.split.engine.evaluator;

import io.split.client.dtos.ConditionType;
import io.split.client.exceptions.ChangeNumberExceptionWrapper;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.splitter.Splitter;
import io.split.grammar.Treatments;
import io.split.storages.SegmentCacheConsumer;
import io.split.storages.SplitCacheConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class EvaluatorImp implements Evaluator {
    private static final Logger _log = LoggerFactory.getLogger(EvaluatorImp.class);

    private final SegmentCacheConsumer _segmentCacheConsumer;
    private final EvaluationContext _evaluationContext;
    private final SplitCacheConsumer _splitCacheConsumer;

    public EvaluatorImp(SplitCacheConsumer splitCacheConsumer, SegmentCacheConsumer segmentCache) {
        _splitCacheConsumer = checkNotNull(splitCacheConsumer);
        _segmentCacheConsumer = checkNotNull(segmentCache);
        _evaluationContext = new EvaluationContext(this, _segmentCacheConsumer);
    }

    @Override
    public TreatmentLabelAndChangeNumber evaluateFeature(String matchingKey, String bucketingKey, String featureFlag, Map<String,
            Object> attributes) {
        ParsedSplit parsedSplit = _splitCacheConsumer.get(featureFlag);
        return evaluateParsedSplit(matchingKey, bucketingKey, attributes, parsedSplit);
    }

    @Override
    public Map<String, TreatmentLabelAndChangeNumber> evaluateFeatures(String matchingKey, String bucketingKey, List<String> featureFlags,
                                                                       Map<String, Object> attributes) {
        Map<String, TreatmentLabelAndChangeNumber> results = new HashMap<>();
        Map<String, ParsedSplit> parsedSplits = _splitCacheConsumer.fetchMany(featureFlags);
        if (parsedSplits == null) {
            return results;
        }
        featureFlags.forEach(s -> results.put(s, evaluateParsedSplit(matchingKey, bucketingKey, attributes, parsedSplits.get(s))));
        return results;
    }

    @Override
    public Map<String, EvaluatorImp.TreatmentLabelAndChangeNumber> evaluateFeaturesByFlagSets(String key, String bucketingKey,
                                                                                              List<String> flagSets, Map<String, Object> attributes) {
        List<String> flagSetsWithNames = getFeatureFlagNamesByFlagSets(flagSets);
        return evaluateFeatures(key, bucketingKey, flagSetsWithNames, attributes);
    }

    private List<String> getFeatureFlagNamesByFlagSets(List<String> flagSets) {
        HashSet<String> ffNamesToReturn = new HashSet<>();
        Map<String, HashSet<String>> namesByFlagSets = _splitCacheConsumer.getNamesByFlagSets(flagSets);
        for (String set: flagSets) {
            HashSet<String> flags = namesByFlagSets.get(set);
            if (flags == null || flags.isEmpty()) {
                _log.warn(String.format("You passed %s Flag Set that does not contain cached feature flag names, please double check " +
                        "what Flag Sets are in use in the Split user interface.", set));
                continue;
            }
            ffNamesToReturn.addAll(flags);
        }
        return new ArrayList<>(ffNamesToReturn);
    }

    /**
     * @param matchingKey  MUST NOT be null
     * @param bucketingKey
     * @param parsedSplit  MUST NOT be null
     * @param attributes   MUST NOT be null
     * @return
     * @throws ChangeNumberExceptionWrapper
     */
    private TreatmentLabelAndChangeNumber getTreatment(String matchingKey, String bucketingKey, ParsedSplit parsedSplit, Map<String,
            Object> attributes) throws ChangeNumberExceptionWrapper {
        try {
            if (parsedSplit.killed()) {
                String config = parsedSplit.configurations() != null ? parsedSplit.configurations().get(parsedSplit.defaultTreatment()) : null;
                return new TreatmentLabelAndChangeNumber(
                        parsedSplit.defaultTreatment(),
                        Labels.KILLED,
                        parsedSplit.changeNumber(),
                        config,
                        parsedSplit.impressionsDisabled());
            }

            /*
             * There are three parts to a single Feature flag: 1) Whitelists 2) Traffic Allocation
             * 3) Rollout. The flag inRollout is there to understand when we move into the Rollout
             * section. This is because we need to make sure that the Traffic Allocation
             * computation happens after the whitelist but before the rollout.
             */
            boolean inRollout = false;

            String bk = (bucketingKey == null) ? matchingKey : bucketingKey;

            for (ParsedCondition parsedCondition : parsedSplit.parsedConditions()) {

                if (!inRollout && parsedCondition.conditionType() == ConditionType.ROLLOUT) {

                    if (parsedSplit.trafficAllocation() < 100) {
                        // if the traffic allocation is 100%, no need to do anything special.
                        int bucket = Splitter.getBucket(bk, parsedSplit.trafficAllocationSeed(), parsedSplit.algo());

                        if (bucket > parsedSplit.trafficAllocation()) {
                            // out of split
                            String config = parsedSplit.configurations() != null ?
                                    parsedSplit.configurations().get(parsedSplit.defaultTreatment()) : null;
                            return new TreatmentLabelAndChangeNumber(parsedSplit.defaultTreatment(), Labels.NOT_IN_SPLIT,
                                    parsedSplit.changeNumber(), config, parsedSplit.impressionsDisabled());
                        }

                    }
                    inRollout = true;
                }

                if (parsedCondition.matcher().match(matchingKey, bucketingKey, attributes, _evaluationContext)) {
                    String treatment = Splitter.getTreatment(bk, parsedSplit.seed(), parsedCondition.partitions(), parsedSplit.algo());
                    String config = parsedSplit.configurations() != null ? parsedSplit.configurations().get(treatment) : null;
                    return new TreatmentLabelAndChangeNumber(
                            treatment,
                            parsedCondition.label(),
                            parsedSplit.changeNumber(),
                            config,
                            parsedSplit.impressionsDisabled());
                }
            }

            String config = parsedSplit.configurations() != null ? parsedSplit.configurations().get(parsedSplit.defaultTreatment()) : null;
            return new TreatmentLabelAndChangeNumber(
                    parsedSplit.defaultTreatment(),
                    Labels.DEFAULT_RULE,
                    parsedSplit.changeNumber(),
                    config,
                    parsedSplit.impressionsDisabled());
        } catch (Exception e) {
            throw new ChangeNumberExceptionWrapper(e, parsedSplit.changeNumber());
        }
    }

    private TreatmentLabelAndChangeNumber evaluateParsedSplit(String matchingKey, String bucketingKey, Map<String, Object> attributes,
                                                              ParsedSplit parsedSplit) {
        try {
            if (parsedSplit == null) {
                return new TreatmentLabelAndChangeNumber(Treatments.CONTROL, Labels.DEFINITION_NOT_FOUND);
            }

            return getTreatment(matchingKey, bucketingKey, parsedSplit, attributes);
        } catch (ChangeNumberExceptionWrapper e) {
            _log.error("Evaluator Exception", e.wrappedException());
            return new EvaluatorImp.TreatmentLabelAndChangeNumber(Treatments.CONTROL, Labels.EXCEPTION, e.changeNumber());
        } catch (Exception e) {
            _log.error("Evaluator Exception", e);
            return new EvaluatorImp.TreatmentLabelAndChangeNumber(Treatments.CONTROL, Labels.EXCEPTION);
        }
    }

    public static final class TreatmentLabelAndChangeNumber {
        public final String treatment;
        public final String label;
        public final Long changeNumber;
        public final String configurations;
        public final boolean track;

        public TreatmentLabelAndChangeNumber(String treatment, String label) {
            this(treatment, label, null, null, true);
        }

        public TreatmentLabelAndChangeNumber(String treatment, String label, Long changeNumber) {
            this(treatment, label, changeNumber, null, true);
        }

        public TreatmentLabelAndChangeNumber(String treatment, String label, Long changeNumber, String configurations, boolean track) {
            this.treatment = treatment;
            this.label = label;
            this.changeNumber = changeNumber;
            this.configurations = configurations;
            this.track = track;
        }
    }
}