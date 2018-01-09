package io.split.client;

import com.google.common.base.Strings;
import io.split.client.api.Key;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.Event;
import io.split.client.exceptions.ChangeNumberExceptionWrapper;
import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionListener;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.experiments.SplitFetcher;
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
    private static final String DEFAULT_RULE = "default rule";
    private static final String DEFINITION_NOT_FOUND = "definition not found";
    private static final String EXCEPTION = "exception";
    private static final String KILLED = "killed";

    private final SplitFactory _container;
    private final SplitFetcher _splitFetcher;
    private final ImpressionListener _impressionListener;
    private final Metrics _metrics;
    private final SplitClientConfig _config;
    private final EventClient _eventClient;

    public SplitClientImpl(SplitFactory container, SplitFetcher splitFetcher, ImpressionListener impressionListener, Metrics metrics, EventClient eventClient, SplitClientConfig config) {
        _container = container;
        _splitFetcher = splitFetcher;
        _impressionListener = impressionListener;
        _metrics = metrics;
        _eventClient = eventClient;
        _config = config;

        checkNotNull(_splitFetcher);
        checkNotNull(_impressionListener);
    }

    @Override
    public void destroy() {
        _container.destroy();
    }

    @Override
    public String getTreatment(String key, String split) {
        return getTreatment(key, split, Collections.<String, Object>emptyMap());
    }

    @Override
    public String getTreatment(String key, String split, Map<String, Object> attributes) {
        return getTreatment(key, null, split, attributes);
    }

    @Override
    public String getTreatment(Key key, String split, Map<String, Object> attributes) {
        if (key == null) {
            _log.warn("key object was null for feature: " + split);
            return Treatments.CONTROL;
        }

        if (key.matchingKey() == null || key.bucketingKey() == null) {
            _log.warn("key object had null matching or bucketing key: " + split);
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

            if (split == null) {
                _log.warn("split was null for key: " + matchingKey);
                return Treatments.CONTROL;
            }

            long start = System.currentTimeMillis();

            TreatmentLabelAndChangeNumber result = getTreatmentResultWithoutImpressions(matchingKey, bucketingKey, split, attributes);

            recordStats(
                    matchingKey,
                    bucketingKey,
                    split,
                    start,
                    result._treatment,
                    "sdk.getTreatment",
                    _config.labelsEnabled() ? result._label : null,
                    result._changeNumber,
                    attributes
            );

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

    private void recordStats(String matchingKey, String bucketingKey, String split, long start, String result,
                             String operation, String label, Long changeNumber, Map<String, Object> attributes) {
        try {
            _impressionListener.log(new Impression(matchingKey, bucketingKey, split, result, System.currentTimeMillis(), label, changeNumber, attributes));
            _metrics.time(operation, System.currentTimeMillis() - start);
        } catch (Throwable t) {
            _log.error("Exception", t);
        }
    }

    public String getTreatmentWithoutImpressions(String matchingKey, String bucketingKey, String split, Map<String, Object> attributes) {
        return getTreatmentResultWithoutImpressions(matchingKey, bucketingKey, split, attributes)._treatment;
    }

    private TreatmentLabelAndChangeNumber getTreatmentResultWithoutImpressions(String matchingKey, String bucketingKey, String split, Map<String, Object> attributes) {
        TreatmentLabelAndChangeNumber result;
        try {
            result = getTreatmentWithoutExceptionHandling(matchingKey, bucketingKey, split, attributes);
        } catch (ChangeNumberExceptionWrapper e) {
            result = new TreatmentLabelAndChangeNumber(Treatments.CONTROL, EXCEPTION, e.changeNumber());
            _log.error("Exception", e.wrappedException());
        } catch (Exception e) {
            result = new TreatmentLabelAndChangeNumber(Treatments.CONTROL, EXCEPTION);
            _log.error("Exception", e);
        }

        return result;
    }

    private TreatmentLabelAndChangeNumber getTreatmentWithoutExceptionHandling(String matchingKey, String bucketingKey, String split, Map<String, Object> attributes) throws ChangeNumberExceptionWrapper {
        ParsedSplit parsedSplit = _splitFetcher.fetch(split);

        if (parsedSplit == null) {
            if (_log.isDebugEnabled()) {
                _log.debug("Returning control because no split was found for: " + split);
            }
            return new TreatmentLabelAndChangeNumber(Treatments.CONTROL, DEFINITION_NOT_FOUND);
        }

        return getTreatment(matchingKey, bucketingKey, parsedSplit, attributes);
    }

    /**
     * @param matchingKey  MUST NOT be null
     * @param bucketingKey
     * @param parsedSplit  MUST NOT be null
     * @param attributes   MUST NOT be null
     * @return
     * @throws ChangeNumberExceptionWrapper
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

            String bk = (bucketingKey == null) ? matchingKey : bucketingKey;

            for (ParsedCondition parsedCondition : parsedSplit.parsedConditions()) {

                if (!inRollout && parsedCondition.conditionType() == ConditionType.ROLLOUT) {

                    if (parsedSplit.trafficAllocation() < 100) {
                        // if the traffic allocation is 100%, no need to do anything special.
                        int bucket = Splitter.getBucket(bk, parsedSplit.trafficAllocationSeed(), parsedSplit.algo());

                        if (bucket >= parsedSplit.trafficAllocation()) {
                            // out of split
                            return new TreatmentLabelAndChangeNumber(parsedSplit.defaultTreatment(), NOT_IN_SPLIT, parsedSplit.changeNumber());
                        }

                    }
                    inRollout = true;
                }

                if (parsedCondition.matcher().match(matchingKey, bucketingKey, attributes, this)) {
                    String treatment = Splitter.getTreatment(bk, parsedSplit.seed(), parsedCondition.partitions(), parsedSplit.algo());
                    return new TreatmentLabelAndChangeNumber(treatment, parsedCondition.label(), parsedSplit.changeNumber());
                }
            }

            return new TreatmentLabelAndChangeNumber(parsedSplit.defaultTreatment(), DEFAULT_RULE, parsedSplit.changeNumber());
        } catch (Exception e) {
            throw new ChangeNumberExceptionWrapper(e, parsedSplit.changeNumber());
        }

    }

    @Override
    public boolean track(String key, String trafficType, String eventType) {
        Event event = createEvent(key, trafficType, eventType);
        return track(event);
    }

    @Override
    public boolean track(String key, String trafficType, String eventType, double value) {
        Event event = createEvent(key, trafficType, eventType);
        event.value = value;

        return track(event);
    }

    private Event createEvent(String key, String trafficType, String eventType) {
        Event event = new Event();
        event.eventTypeId = eventType;
        event.trafficTypeName = trafficType;
        event.key = key;
        event.timestamp = System.currentTimeMillis();
        return event;
    }

    private boolean track(Event event) {
        if (Strings.isNullOrEmpty(event.trafficTypeName)) {
            _log.warn("Traffic Type was null or empty");
            return false;
        }

        if (Strings.isNullOrEmpty(event.eventTypeId)) {
            _log.warn("Event Type was null or empty");
            return false;
        }

        if (Strings.isNullOrEmpty(event.key)) {
            _log.warn("Cannot track event for null key");
            return false;
        }

        return _eventClient.track(event);

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
