package io.split.client;

import io.split.client.api.Key;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.impressions.TreatmentLog;
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

    private final SplitFetcher _splitFetcher;
    private final TreatmentLog _treatmentLog;
    private final Metrics _metrics;
    private final SplitClientConfig _config;

    public SplitClientImpl(SplitFetcher splitFetcher, TreatmentLog treatmentLog, Metrics metrics, SplitClientConfig config) {
        _splitFetcher = splitFetcher;
        _treatmentLog = treatmentLog;
        _metrics = metrics;
        _config = config;

        checkNotNull(_splitFetcher);
        checkNotNull(_treatmentLog);
    }

    @Override
    public String getTreatment(String key, String feature) {
        return getTreatment(key, feature, Collections.<String, Object>emptyMap());
    }

    @Override
    public String getTreatment(String key, String feature, Map<String, Object> attributes) {
        return getTreatment(key, key, feature, attributes);
    }

    @Override
    public String getTreatment(Key key, String feature, Map<String, Object> attributes) {
        if (key == null) {
            _log.warn("key object was null for feature: " + feature);
            return Treatments.CONTROL;
        }

        return getTreatment(key.matchingKey(), key.bucketingKey(), feature, attributes);
    }

    private String getTreatment(String matchingKey, String bucketingKey, String feature, Map<String, Object> attributes) {
        try {
            if (matchingKey == null) {
                _log.warn("matchingKey was null for feature: " + feature);
                return Treatments.CONTROL;
            }

            if (bucketingKey == null) {
                _log.warn("bucketingKey was null for feature: " + feature);
                return Treatments.CONTROL;
            }

            if (feature == null) {
                _log.warn("feature was null for key: " + matchingKey);
                return Treatments.CONTROL;
            }

            long start = System.currentTimeMillis();

            TreatmentLabelAndChangeNumber result = null;
            try {
                result = getTreatmentWithoutExceptionHandling(matchingKey, bucketingKey, feature, attributes);
            } catch (ChangeNumberExceptionWrapper e) {
                result = new TreatmentLabelAndChangeNumber(Treatments.CONTROL, "exception", e.changeNumber());
                _log.error("Exception", e.wrappedException());
            } catch (Exception e) {
                result = new TreatmentLabelAndChangeNumber(Treatments.CONTROL, "exception");
                _log.error("Exception", e);
            } finally {
                recordStats(
                        matchingKey,
                        bucketingKey,
                        feature,
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

    private void recordStats(String matchingKey, String bucketingKey, String feature, long start, String result, String operation, String label, Long changeNumber) {
        try {
            _treatmentLog.log(matchingKey, bucketingKey, feature, result, System.currentTimeMillis(), label, changeNumber);
            _metrics.time(operation, System.currentTimeMillis() - start);
        } catch (Throwable t) {
            _log.error("Exception", t);
        }
    }

    private TreatmentLabelAndChangeNumber getTreatmentWithoutExceptionHandling(String matchingKey, String bucketingKey, String feature, Map<String, Object> attributes) throws ChangeNumberExceptionWrapper {
        ParsedSplit parsedSplit = _splitFetcher.fetch(feature);

        if (parsedSplit == null) {
            if (_log.isDebugEnabled()) {
                _log.debug("Returning control because no split was found for: " + feature);
            }
            return new TreatmentLabelAndChangeNumber(Treatments.CONTROL, "rules not found");
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
                return new TreatmentLabelAndChangeNumber(parsedSplit.defaultTreatment(), "killed", parsedSplit.changeNumber());
            }

            for (ParsedCondition parsedCondition : parsedSplit.parsedConditions()) {
                if (parsedCondition.matcher().match(matchingKey, attributes)) {
                    String treatment = Splitter.getTreatment(bucketingKey, parsedSplit.seed(), parsedCondition.partitions());
                    return new TreatmentLabelAndChangeNumber(treatment, parsedCondition.label(), parsedSplit.changeNumber());
                }
            }

            return new TreatmentLabelAndChangeNumber(parsedSplit.defaultTreatment(), "no rule matched", parsedSplit.changeNumber());
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
