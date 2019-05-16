package io.split.client;

import com.google.common.annotations.VisibleForTesting;
import io.split.client.api.Key;
import io.split.client.api.SplitResult;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.Event;
import io.split.client.exceptions.ChangeNumberExceptionWrapper;
import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionListener;
import io.split.engine.SDKReadinessGates;
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
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A basic implementation of SplitClient.
 *
 * @author adil
 */
public final class SplitClientImpl implements SplitClient {

    private static final Logger _log = LoggerFactory.getLogger(SplitClientImpl.class);

    private static final String GET_TREATMENT_LABEL = "sdk.getTreatment";
    private static final String GET_TREATMENT_CONFIG_LABEL = "sdk.getTreatmentWithConfig";

    private static final String NOT_IN_SPLIT = "not in split";
    private static final String DEFAULT_RULE = "default rule";
    private static final String DEFINITION_NOT_FOUND = "definition not found";
    private static final String EXCEPTION = "exception";
    private static final String KILLED = "killed";

    public static final Pattern EVENT_TYPE_MATCHER = Pattern.compile("^[a-zA-Z0-9][-_.:a-zA-Z0-9]{0,79}$");

    public static final SplitResult SPLIT_RESULT_CONTROL = new SplitResult(Treatments.CONTROL, null);

    private final SplitFactory _container;
    private final SplitFetcher _splitFetcher;
    private final ImpressionListener _impressionListener;
    private final Metrics _metrics;
    private final SplitClientConfig _config;
    private final EventClient _eventClient;
    private final SDKReadinessGates _gates;


    public SplitClientImpl(SplitFactory container,
                           SplitFetcher splitFetcher,
                           ImpressionListener impressionListener,
                           Metrics metrics,
                           EventClient eventClient,
                           SplitClientConfig config,
                           SDKReadinessGates gates) {
        _container = container;
        _splitFetcher = splitFetcher;
        _impressionListener = impressionListener;
        _metrics = metrics;
        _eventClient = eventClient;
        _config = config;
        _gates = gates;

        checkNotNull(gates);
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
            _log.error("getTreatment: you passed a null key, the key must be a non-empty string");
            return Treatments.CONTROL;
        }

        if (key.matchingKey() == null) {
            _log.error("getTreatment: you passed a null matchingKey, the matchingKey must be a non-empty string");
            return Treatments.CONTROL;
        }


        if (key.bucketingKey() == null) {
            _log.error("getTreatment: you passed a null bucketingKey, the bucketingKey must be a non-empty string");
            return Treatments.CONTROL;
        }

        return getTreatment(key.matchingKey(), key.bucketingKey(), split, attributes);
    }

    private String getTreatment(String matchingKey, String bucketingKey, String split, Map<String, Object> attributes) {
        return getTreatmentWithConfigInternal(GET_TREATMENT_LABEL, matchingKey, bucketingKey, split, attributes).treatment();
    }

    @Override
    public SplitResult getTreatmentWithConfig(String key, String split) {
        return getTreatmentWithConfigInternal(GET_TREATMENT_LABEL, key, null, split, Collections.<String, Object>emptyMap());
    }

    @Override
    public SplitResult getTreatmentWithConfig(String key, String split, Map<String, Object> attributes) {
        return getTreatmentWithConfigInternal(GET_TREATMENT_LABEL, key, null, split, attributes);
    }

    @Override
    public SplitResult getTreatmentWithConfig(Key key, String split, Map<String, Object> attributes) {
        if (key == null) {
            _log.error("getTreatment: you passed a null key, the key must be a non-empty string");
            return SPLIT_RESULT_CONTROL;
        }

        if (key.matchingKey() == null) {
            _log.error("getTreatment: you passed a null matchingKey, the matchingKey must be a non-empty string");
            return SPLIT_RESULT_CONTROL;
        }


        if (key.bucketingKey() == null) {
            _log.error("getTreatment: you passed a null bucketingKey, the bucketingKey must be a non-empty string");
            return SPLIT_RESULT_CONTROL;
        }

        return getTreatmentWithConfigInternal(GET_TREATMENT_LABEL, key.matchingKey(), key.bucketingKey(), split, attributes);
    }

    private SplitResult getTreatmentWithConfigInternal(String label, String matchingKey, String bucketingKey, String split, Map<String, Object> attributes) {
        try {
            if (_container.isDestroyed()) {
                _log.error("Client has already been destroyed - no calls possible");
                return SPLIT_RESULT_CONTROL;
            }

            if (matchingKey == null) {
                _log.error("getTreatmentWithConfig: you passed a null matchingKey, the matchingKey must be a non-empty string");
                return SPLIT_RESULT_CONTROL;
            }
            if (matchingKey.length() > _config.maxStringLength()) {
                _log.error("getTreatmentWithConfig: matchingKey too long - must be " + _config.maxStringLength() + " characters or less");
                return SPLIT_RESULT_CONTROL;
            }
            if (matchingKey.isEmpty()) {
                _log.error("getTreatmentWithConfig: you passed an empty string, matchingKey must be a non-empty string");
                return SPLIT_RESULT_CONTROL;
            }
            if (bucketingKey != null && bucketingKey.isEmpty()) {
                _log.error("getTreatmentWithConfig: you passed an empty string, bucketingKey must be a non-empty string");
                return SPLIT_RESULT_CONTROL;
            }
            if (bucketingKey != null && bucketingKey.length() > _config.maxStringLength()) {
                _log.error("getTreatmentWithConfig: bucketingKey too long - must be " + _config.maxStringLength() + " characters or less");
                return SPLIT_RESULT_CONTROL;
            }

            if (split == null) {
                _log.error("getTreatmentWithConfig: you passed a null split name, split name must be a non-empty string");
                return SPLIT_RESULT_CONTROL;
            }

            if (split.isEmpty()) {
                _log.error("getTreatmentWithConfig: you passed an empty split name, split name must be a non-empty string");
                return SPLIT_RESULT_CONTROL;
            }

            String trimmed = split.trim();
            if (!trimmed.equals(split)) {
                _log.warn("getTreatmentWithConfig: split name \"" + split + "\" has extra whitespace, trimming");
                split = trimmed;
            }

            long start = System.currentTimeMillis();

            TreatmentLabelAndChangeNumber result = getTreatmentResultWithoutImpressions(matchingKey, bucketingKey, split, attributes);

            recordStats(
                    matchingKey,
                    bucketingKey,
                    split,
                    start,
                    result._treatment,
                    label,
                    _config.labelsEnabled() ? result._label : null,
                    result._changeNumber,
                    attributes
            );

            return new SplitResult(result._treatment, result._configurations);
        } catch (Exception e) {
            try {
                _log.error("CatchAll Exception", e);
            } catch (Exception e1) {
                // ignore
            }
            return SPLIT_RESULT_CONTROL;
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

    @VisibleForTesting
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
            if (_gates.isSDKReadyNow()) {
                _log.error(
                        "getTreatment: you passed \"" + split + "\" that does not exist in this environment, " +
                        "please double check what Splits exist in the web console.");
            }
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
                String config = parsedSplit.configurations() != null ? parsedSplit.configurations().get(parsedSplit.defaultTreatment()) : null;
                return new TreatmentLabelAndChangeNumber(parsedSplit.defaultTreatment(), KILLED, parsedSplit.changeNumber(), config);
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

                        if (bucket > parsedSplit.trafficAllocation()) {
                            // out of split
                            String config = parsedSplit.configurations() != null ? parsedSplit.configurations().get(parsedSplit.defaultTreatment()) : null;
                            return new TreatmentLabelAndChangeNumber(parsedSplit.defaultTreatment(), NOT_IN_SPLIT, parsedSplit.changeNumber(), config);
                        }

                    }
                    inRollout = true;
                }

                if (parsedCondition.matcher().match(matchingKey, bucketingKey, attributes, this)) {
                    String treatment = Splitter.getTreatment(bk, parsedSplit.seed(), parsedCondition.partitions(), parsedSplit.algo());
                    String config = parsedSplit.configurations() != null ? parsedSplit.configurations().get(treatment) : null;
                    return new TreatmentLabelAndChangeNumber(treatment, parsedCondition.label(), parsedSplit.changeNumber(), config);
                }
            }

            String config = parsedSplit.configurations() != null ? parsedSplit.configurations().get(parsedSplit.defaultTreatment()) : null;
            return new TreatmentLabelAndChangeNumber(parsedSplit.defaultTreatment(), DEFAULT_RULE, parsedSplit.changeNumber(), config);
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

    @Override
    public boolean track(String key, String trafficType, String eventType, Map<String, Object> properties) {
        Event event = createEvent(key, trafficType, eventType);
        event.properties = properties;
        return track(event);
    }

    @Override
    public boolean track(String key, String trafficType, String eventType, double value, Map<String, Object> properties) {
        Event event = createEvent(key, trafficType, eventType);
        event.properties = properties;
        return track(event);
    }

    @Override
    public void blockUntilReady() throws TimeoutException, InterruptedException {
        long startTime = System.currentTimeMillis();
        if (_config.blockUntilReady() <= 0) {
            throw new IllegalArgumentException("setBlockUntilReadyTimeout must be positive but in config was: " + _config.blockUntilReady());
        }
        if (!_gates.isSDKReady(_config.blockUntilReady())) {
            throw new TimeoutException("SDK was not ready in " + _config.blockUntilReady()+ " milliseconds");
        }
        _log.debug(String.format("Split SDK ready in %d ms", (System.currentTimeMillis() - startTime)));
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
        if (_container.isDestroyed()) {
            _log.error("Client has already been destroyed - no calls possible");
            return false;
        }
        // Traffic Type validations
        if (event.trafficTypeName == null) {
            _log.error("track: you passed a null trafficTypeName, trafficTypeName must be a non-empty string");
            return false;
        }

        if (event.trafficTypeName.isEmpty()) {
            _log.error("track: you passed an empty trafficTypeName, trafficTypeName must be a non-empty string");
            return false;
        }

        if (!event.trafficTypeName.equals(event.trafficTypeName.toLowerCase())) {
            _log.warn("track: trafficTypeName should be all lowercase - converting string to lowercase");
            event.trafficTypeName = event.trafficTypeName.toLowerCase();
        }

        if (!_splitFetcher.fetchKnownTrafficTypes().contains(event.trafficTypeName)) {
            _log.warn("track: Traffic Type " + event.trafficTypeName + " does not have any corresponding Splits in this environment, " +
                    "make sure you’re tracking your events to a valid traffic type defined in the Split console.");
        }

        // EventType validations
        if (event.eventTypeId == null) {
            _log.error("track: you passed a null eventTypeId, eventTypeId must be a non-empty string");
            return false;
        }

        if (event.eventTypeId.isEmpty()) {
            _log.error("track:you passed an empty eventTypeId, eventTypeId must be a non-empty string");
            return false;
        }

        if (!EVENT_TYPE_MATCHER.matcher(event.eventTypeId).find()) {
            _log.error("track: you passed " + event.eventTypeId + ", eventTypeId must adhere to the regular expression " +
                    "[a-zA-Z0-9][-_.:a-zA-Z0-9]{0,79}. This means an eventTypeID must be alphanumeric, " +
                    "cannot be more than 80 characters long, and can only include a dash, underscore, period, " +
                    "or colon as separators of alphanumeric characters");
            return false;
        }

            // Key Validations
        if (event.key == null) {
            _log.error("track: you passed a null key, key must be a non-empty string");
            return false;
        }

        if (event.key.isEmpty()) {
            _log.error("track: you passed an empty key, key must be a non-empty string");
            return false;
        }

        if (event.key.length() > _config.maxStringLength()) {
            _log.error("track: key too long - must be " + _config.maxStringLength() + "characters or less");
            return false;
        }

        int size = 1024; // We assume 1kb events without properties (750 bytes avg measured)
        if (null != event.properties) {
            if (event.properties.size() > 300) {
                // TODO: Log error
                return false;
            }

            for (Map.Entry<String, Object> entry: event.properties.entrySet()) {
                size += entry.getKey().length();
                Object value = entry.getValue();
                if (null == value) {
                    continue;
                }

                if (!(value instanceof Number) && !(value instanceof Boolean) && !(value instanceof String)) {
                    entry.setValue(null);
                }

                if (value instanceof String) {
                    size += ((String) value).length();
                }

                if (size > Event.MAX_PROPERTIES_LENGTH_BYTES) {
                    // TODO: Log error
                    return false;
                }
            }

        }

        return _eventClient.track(event, size);
    }

    private static final class TreatmentLabelAndChangeNumber {
        private final String _treatment;
        private final String _label;
        private final Long _changeNumber;
        private final String _configurations;

        public TreatmentLabelAndChangeNumber(String treatment, String label) {
            this(treatment, label, null, null);
        }

        public TreatmentLabelAndChangeNumber(String treatment, String label, Long changeNumber) {
            this(treatment, label, changeNumber, null);
        }

        public TreatmentLabelAndChangeNumber(String treatment, String label, Long changeNumber, String configurations) {
            _treatment = treatment;
            _label = label;
            _changeNumber = changeNumber;
            _configurations = configurations;
        }
    }
}
