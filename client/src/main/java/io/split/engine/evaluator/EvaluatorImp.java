package io.split.engine.evaluator;

import io.split.client.dtos.FallbackTreatment;
import io.split.client.dtos.FallbackTreatmentCalculator;
import io.split.client.exceptions.ChangeNumberExceptionWrapper;
import io.split.engine.experiments.ParsedSplit;
import io.split.rules.engine.EvaluationResult;
import io.split.rules.engine.TargetingEngine;
import io.split.rules.engine.TargetingEngineImpl;
import io.split.rules.exceptions.VersionedExceptionWrapper;
import io.split.storages.RuleBasedSegmentCacheConsumer;
import io.split.storages.SegmentCacheConsumer;
import io.split.storages.SplitCacheConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import java.util.Objects;

public class EvaluatorImp implements Evaluator {
    private static final Logger _log = LoggerFactory.getLogger(EvaluatorImp.class);

    private final SegmentCacheConsumer _segmentCacheConsumer;
    private final EvaluationContext _evaluationContext;
    private final SplitCacheConsumer _splitCacheConsumer;
    private final FallbackTreatmentCalculator _fallbackTreatmentCalculator;
    private final TargetingEngine _targetingEngine;
    private final String _evaluatorException = "Evaluator Exception";

    public EvaluatorImp(SplitCacheConsumer splitCacheConsumer, SegmentCacheConsumer segmentCache,
                        RuleBasedSegmentCacheConsumer ruleBasedSegmentCacheConsumer,
                        FallbackTreatmentCalculator fallbackTreatmentCalculator) {
        _splitCacheConsumer = Objects.requireNonNull(splitCacheConsumer);
        _segmentCacheConsumer = Objects.requireNonNull(segmentCache);
        _evaluationContext = new EvaluationContext(this, _segmentCacheConsumer, ruleBasedSegmentCacheConsumer);
        _fallbackTreatmentCalculator = fallbackTreatmentCalculator;
        _targetingEngine = new TargetingEngineImpl();
    }

    @Override
    public TreatmentLabelAndChangeNumber evaluateFeature(String matchingKey, String bucketingKey, String featureFlag, Map<String,
            Object> attributes) {
        ParsedSplit parsedSplit = _splitCacheConsumer.get(featureFlag);
        return evaluateParsedSplit(matchingKey, bucketingKey, attributes, parsedSplit, featureFlag);
    }

    @Override
    public Map<String, TreatmentLabelAndChangeNumber> evaluateFeatures(String matchingKey, String bucketingKey, List<String> featureFlags,
                                                                       Map<String, Object> attributes) {
        Map<String, TreatmentLabelAndChangeNumber> results = new HashMap<>();
        Map<String, ParsedSplit> parsedSplits = _splitCacheConsumer.fetchMany(featureFlags);
        if (parsedSplits == null) {
            return results;
        }
        featureFlags.forEach(s -> results.put(s, evaluateParsedSplit(matchingKey, bucketingKey, attributes, parsedSplits.get(s), s)));
        return results;
    }

    @Override
    public Map<String, EvaluatorImp.TreatmentLabelAndChangeNumber> evaluateFeaturesByFlagSets(String key, String bucketingKey,
                                                                                              List<String> flagSets, Map<String, Object> attributes) {
        List<String> flagSetsWithNames = getFeatureFlagNamesByFlagSets(flagSets);
        try {
            return evaluateFeatures(key, bucketingKey, flagSetsWithNames, attributes);
        } catch (Exception e) {
            _log.error(_evaluatorException, e);
            return createMapControl(flagSetsWithNames, io.split.engine.evaluator.Labels.EXCEPTION);
        }
    }

    private Map<String, EvaluatorImp.TreatmentLabelAndChangeNumber> createMapControl(List<String> featureFlags, String label) {
        Map<String, TreatmentLabelAndChangeNumber> result = new HashMap<>();
        featureFlags.forEach(s -> result.put(s, checkFallbackTreatment(s, label)));
        return result;
    }

    private EvaluatorImp.TreatmentLabelAndChangeNumber checkFallbackTreatment(String featureName, String label) {
        FallbackTreatment fallbackTreatment = _fallbackTreatmentCalculator.resolve(featureName, label);
        return new EvaluatorImp.TreatmentLabelAndChangeNumber(fallbackTreatment.getTreatment(),
                fallbackTreatment.getLabel(),
                null,
                getFallbackConfig(fallbackTreatment),
                false);
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
     * @param bucketingKey may be null
     * @param parsedSplit  MUST NOT be null
     * @param attributes   may be null
     * @return
     * @throws ChangeNumberExceptionWrapper
     */
    private TreatmentLabelAndChangeNumber getTreatment(String matchingKey, String bucketingKey, ParsedSplit parsedSplit,
                                                       Map<String, Object> attributes) throws ChangeNumberExceptionWrapper {
        try {
            EvaluationResult r = _targetingEngine.evaluate(matchingKey, bucketingKey,
                    parsedSplit.targetingRule(), attributes, _evaluationContext);
            return new TreatmentLabelAndChangeNumber(r.treatment, r.label, r.version, r.config, r.impressionsDisabled);
        } catch (VersionedExceptionWrapper e) {
            throw new ChangeNumberExceptionWrapper(e.wrappedException(), e.version());
        }
    }

    private String getFallbackConfig(FallbackTreatment fallbackTreatment) {
        if (fallbackTreatment.getConfig() != null) {
            return fallbackTreatment.getConfig();
        }

        return null;
    }

    private TreatmentLabelAndChangeNumber evaluateParsedSplit(String matchingKey, String bucketingKey, Map<String, Object> attributes,
                                                              ParsedSplit parsedSplit, String featureName) {
        try {
            if (parsedSplit == null) {
                FallbackTreatment fallbackTreatment = _fallbackTreatmentCalculator.resolve(featureName, Labels.DEFINITION_NOT_FOUND);
                return new TreatmentLabelAndChangeNumber(fallbackTreatment.getTreatment(),
                        fallbackTreatment.getLabel(),
                        null,
                        getFallbackConfig(fallbackTreatment),
                        false);
            }
            return getTreatment(matchingKey, bucketingKey, parsedSplit, attributes);
        } catch (ChangeNumberExceptionWrapper e) {
            _log.error(_evaluatorException, e.wrappedException());
            FallbackTreatment fallbackTreatment = _fallbackTreatmentCalculator.resolve(featureName, Labels.EXCEPTION);
            return new TreatmentLabelAndChangeNumber(fallbackTreatment.getTreatment(), fallbackTreatment.getLabel(), e.changeNumber());
        } catch (Exception e) {
            _log.error(_evaluatorException, e);
            FallbackTreatment fallbackTreatment = _fallbackTreatmentCalculator.resolve(featureName, Labels.EXCEPTION);
            return new TreatmentLabelAndChangeNumber(fallbackTreatment.getTreatment(), fallbackTreatment.getLabel());
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