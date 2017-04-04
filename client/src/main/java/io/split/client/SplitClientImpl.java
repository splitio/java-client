package io.split.client;

import io.split.client.api.Key;
import io.split.client.dtos.ConditionType;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.experiments.SplitFetcher;
import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionListener;
import io.split.engine.metrics.Metrics;
import io.split.engine.splitter.Splitter;
import io.split.grammar.Treatments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A basic implementation of SplitClient.
 *
 * @author adil
 */
public final class SplitClientImpl implements SplitClient {

    private static final Logger _log = LoggerFactory.getLogger(SplitClientImpl.class);

    private static final String NOT_IN_SPLIT = "not in split";
    private static final String NO_RULE_MATCHED = "no rule matched";
    private static final String RULES_NOT_FOUND = "rules not found";
    private static final String EXCEPTION = "exception";
    private static final String KILLED = "killed";

    private final SplitFetcher _splitFetcher;
    private final ImpressionListener _impressionListener;
    private final Metrics _metrics;
    private final SplitClientConfig _config;

    public SplitClientImpl(SplitFetcher splitFetcher, ImpressionListener impressionListener, Metrics metrics, SplitClientConfig config) {
        _splitFetcher = splitFetcher;
        _impressionListener = impressionListener;
        _metrics = metrics;
        _config = config;

        checkNotNull(_splitFetcher);
        checkNotNull(_impressionListener);
    }

    @Override
    public String getTreatment(String key, String split) {
        return getTreatment(key, split, Collections.<String, Object>emptyMap());
    }

    @Override
    public String getTreatment(String key, String split, Map<String, Object> attributes) {
        return getTreatment(key, key, split, attributes);
    }

    @Override
    public String getTreatment(Key key, String split, Map<String, Object> attributes) {
        if (key == null) {
            _log.warn("key object was null for feature: " + split);
            return Treatments.CONTROL;
        }

        return getTreatment(key.matchingKey(), key.bucketingKey(), split, attributes);
    }

    private String getTreatment(String matchingKey, String bucketingKey, String split, Map<String, Object> attributes) {
        try {
            if (matchingKey == null) {
                _log.warn("matchingKey was null for split: " + split);
                return Treatments.CONTROL;
            }

            if (bucketingKey == null) {
                _log.warn("bucketingKey was null for split: " + split);
                return Treatments.CONTROL;
            }

            if (split == null) {
                _log.warn("split was null for key: " + matchingKey);
                return Treatments.CONTROL;
            }

            long start = System.currentTimeMillis();

            TreatmentLabelAndChangeNumber result = null;
            try {
                result = getTreatmentWithoutExceptionHandling(matchingKey, bucketingKey, split, attributes);
            } catch (ChangeNumberExceptionWrapper e) {
                result = new TreatmentLabelAndChangeNumber(Treatments.CONTROL, EXCEPTION, e.changeNumber());
                _log.error("Exception", e.wrappedException());
            } catch (Exception e) {
                result = new TreatmentLabelAndChangeNumber(Treatments.CONTROL, EXCEPTION);
                _log.error("Exception", e);
            } finally {
                recordStats(
                        matchingKey,
                        bucketingKey,
                        split,
                        start,
                        result._treatment,
                        "sdk.getTreatment",
                        _config.labelsEnabled() ? result._label : null,
                        result._changeNumber);
            }

            return result._treatment;

        } catch (Exception e) {
            try {
                _log.error("CatchAll Exception", e);
            } catch (Exception e1) {
                // ignore
            }
            return Treatments.CONTROL;
        }
    }

    private void recordStats(String matchingKey, String bucketingKey, String split, long start, String result, String operation, String label, Long changeNumber) {
        try {
            _impressionListener.log(new Impression(matchingKey, bucketingKey, split, result, System.currentTimeMillis(), label, changeNumber));
            _metrics.time(operation, System.currentTimeMillis() - start);
        } catch (Throwable t) {
            _log.error("Exception", t);
        }
    }

    private TreatmentLabelAndChangeNumber getTreatmentWithoutExceptionHandling(String matchingKey, String bucketingKey, String split, Map<String, Object> attributes) throws ChangeNumberExceptionWrapper {
        ParsedSplit parsedSplit = _splitFetcher.fetch(split);

        if (parsedSplit == null) {
            if (_log.isDebugEnabled()) {
                _log.debug("Returning control because no split was found for: " + split);
            }
            return new TreatmentLabelAndChangeNumber(Treatments.CONTROL, RULES_NOT_FOUND);
        }

        return getTreatment(matchingKey, bucketingKey, parsedSplit, attributes);
    }

    /**
     * @param matchingKey     MUST NOT be null
     * @param parsedSplit MUST NOT be null
     * @return
     */
    private TreatmentLabelAndChangeNumber getTreatment(String matchingKey, String bucketingKey, ParsedSplit parsedSplit, Map<String, Object> attributes) throws ChangeNumberExceptionWrapper {
        try {
            if (parsedSplit.killed()) {
                return new TreatmentLabelAndChangeNumber(parsedSplit.defaultTreatment(), KILLED, parsedSplit.changeNumber());
            }

            /*
             * There are three parts to a single Split: 1) Whitelists 2) Traffic Allocation
             * 3) Rollout. The flag inRollout is there to understand when we move into the Rollout
             * section. This is because we need to make sure that the Traffic Allocation
             * computation happens after the whitelist but before the rollout.
             */
            boolean inRollout = false;

            for (ParsedCondition parsedCondition : parsedSplit.parsedConditions()) {

                if (!inRollout && parsedCondition.conditionType() == ConditionType.ROLLOUT) {

                    if (parsedSplit.trafficAllocation() < 100) {
                        // if the traffic allocation is 100%, no need to do anything special.
                        int bucket = Splitter.getBucket(bucketingKey, parsedSplit.trafficAllocationSeed());

                        if (bucket >= parsedSplit.trafficAllocation()) {
                            // out of split
                            return new TreatmentLabelAndChangeNumber(parsedSplit.defaultTreatment(), NOT_IN_SPLIT, parsedSplit.changeNumber());
                        }

                    }
                    inRollout = true;
                }

                if (parsedCondition.matcher().match(matchingKey, attributes)) {
                    String treatment = Splitter.getTreatment(bucketingKey, parsedSplit.seed(), parsedCondition.partitions(), parsedSplit.algo());
                    return new TreatmentLabelAndChangeNumber(treatment, parsedCondition.label(), parsedSplit.changeNumber());
                }
            }

            return new TreatmentLabelAndChangeNumber(parsedSplit.defaultTreatment(), NO_RULE_MATCHED, parsedSplit.changeNumber());
        } catch (Exception e) {
            throw new ChangeNumberExceptionWrapper(e, parsedSplit.changeNumber());
        }

    }

    private static final class TreatmentLabelAndChangeNumber {
        private final String _treatment;
        private final String _label;
        private final Long _changeNumber;

        public TreatmentLabelAndChangeNumber(String treatment, String label) {
            this(treatment, label, null);
        }

        public TreatmentLabelAndChangeNumber(String treatment, String label, Long changeNumber) {
            _treatment = treatment;
            _label = label;
            _changeNumber = changeNumber;
        }
    }


}
