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

    public SplitClientImpl(SplitFetcher splitFetcher, TreatmentLog treatmentLog, Metrics metrics) {
        _splitFetcher = splitFetcher;
        _treatmentLog = treatmentLog;
        _metrics = metrics;

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

            TreatmentAndLabel result = null;
            try {
                result = getTreatmentWithoutExceptionHandling(matchingKey, bucketingKey, feature, attributes);
            } catch (Exception e) {
                result = new TreatmentAndLabel(Treatments.CONTROL, "exception");
                _log.error("Exception", e);
            } finally {
                recordStats(matchingKey, bucketingKey, feature, start, result._treatment, "sdk.getTreatment", result._label);
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

    private void recordStats(String matchingKey, String bucketingKey, String feature, long start, String result, String operation, String label) {
        try {
            _treatmentLog.log(matchingKey, bucketingKey, feature, result, System.currentTimeMillis(), label);
            _metrics.time(operation, System.currentTimeMillis() - start);
        } catch (Throwable t) {
            _log.error("Exception", t);
        }
    }

    private TreatmentAndLabel getTreatmentWithoutExceptionHandling(String matchingKey, String bucketingKey, String feature, Map<String, Object> attributes) {
        ParsedSplit parsedSplit = _splitFetcher.fetch(feature);

        if (parsedSplit == null) {
            if (_log.isDebugEnabled()) {
                _log.debug("Returning control because no split was found for: " + feature);
            }
            return new TreatmentAndLabel(Treatments.CONTROL, "split not found");
        }

        return getTreatment(matchingKey, bucketingKey, parsedSplit, attributes);
    }

    /**
     * @param matchingKey     MUST NOT be null
     * @param parsedSplit MUST NOT be null
     * @return
     */
    private TreatmentAndLabel getTreatment(String matchingKey, String bucketingKey, ParsedSplit parsedSplit, Map<String, Object> attributes) {
        if (parsedSplit.killed()) {
            return new TreatmentAndLabel(parsedSplit.defaultTreatment(), "killed");
        }

        for (ParsedCondition parsedCondition : parsedSplit.parsedConditions()) {
            if (parsedCondition.matcher().match(matchingKey, attributes)) {
                String treatment = Splitter.getTreatment(bucketingKey, parsedSplit.seed(), parsedCondition.partitions());
                return new TreatmentAndLabel(treatment, parsedCondition.label());
            }
        }

        return new TreatmentAndLabel(parsedSplit.defaultTreatment(), "no condition matched");
    }

    private static final class TreatmentAndLabel {
        private final String _treatment;
        private final String _label;

        public TreatmentAndLabel(String treatment, String label) {
            _treatment = treatment;
            _label = label;
        }
    }


}
