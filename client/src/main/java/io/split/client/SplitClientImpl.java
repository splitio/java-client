package io.split.client;

import io.split.cache.SplitCache;
import io.split.client.api.Key;
import io.split.client.api.SplitResult;
import io.split.client.dtos.Event;
import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionsManager;
import io.split.engine.SDKReadinessGates;
import io.split.engine.evaluator.Evaluator;
import io.split.engine.evaluator.EvaluatorImp;
import io.split.engine.evaluator.Labels;
import io.split.grammar.Treatments;
import io.split.inputValidation.EventsValidator;
import io.split.inputValidation.KeyValidator;
import io.split.inputValidation.SplitNameValidator;
import io.split.inputValidation.TrafficTypeValidator;
import io.split.telemetry.domain.enums.MethodEnum;
import io.split.telemetry.storage.TelemetryConfigProducer;
import io.split.telemetry.storage.TelemetryEvaluationProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A basic implementation of SplitClient.
 *
 * @author adil
 */
public final class SplitClientImpl implements SplitClient {
    public static final SplitResult SPLIT_RESULT_CONTROL = new SplitResult(Treatments.CONTROL, null);

    private static final String GET_TREATMENT = "getTreatment";
    private static final String GET_TREATMENT_WITH_CONFIG = "getTreatmentWithConfig";

    private static final Logger _log = LoggerFactory.getLogger(SplitClientImpl.class);

    private final SplitFactory _container;
    private final SplitCache _splitCache;
    private final ImpressionsManager _impressionManager;
    private final SplitClientConfig _config;
    private final EventClient _eventClient;
    private final SDKReadinessGates _gates;
    private final Evaluator _evaluator;
    private final TelemetryEvaluationProducer _telemetryEvaluationProducer;
    private final TelemetryConfigProducer _telemetryConfigProducer;

    public SplitClientImpl(SplitFactory container,
                           SplitCache splitCache,
                           ImpressionsManager impressionManager,
                           EventClient eventClient,
                           SplitClientConfig config,
                           SDKReadinessGates gates,
                           Evaluator evaluator,
                           TelemetryEvaluationProducer telemetryEvaluationProducer,
                           TelemetryConfigProducer telemetryConfigProducer) {
        _container = container;
        _splitCache = checkNotNull(splitCache);
        _impressionManager = checkNotNull(impressionManager);
        _eventClient = eventClient;
        _config = config;
        _gates = checkNotNull(gates);
        _evaluator = checkNotNull(evaluator);
        _telemetryEvaluationProducer = checkNotNull(telemetryEvaluationProducer);
        _telemetryConfigProducer = checkNotNull(telemetryConfigProducer);
    }

    @Override
    public String getTreatment(String key, String split) {
        return getTreatment(key, split, Collections.<String, Object>emptyMap());
    }

    @Override
    public String getTreatment(String key, String split, Map<String, Object> attributes) {
        return getTreatmentWithConfigInternal(GET_TREATMENT, key, null, split, attributes, MethodEnum.TREATMENT).treatment();
    }

    @Override
    public String getTreatment(Key key, String split, Map<String, Object> attributes) {
        return getTreatmentWithConfigInternal(GET_TREATMENT, key.matchingKey(), key.bucketingKey(), split, attributes, MethodEnum.TREATMENT).treatment();
    }

    @Override
    public SplitResult getTreatmentWithConfig(String key, String split) {
        return getTreatmentWithConfigInternal(GET_TREATMENT_WITH_CONFIG, key, null, split, Collections.<String, Object>emptyMap(), MethodEnum.TREATMENT_WITH_CONFIG);
    }

    @Override
    public SplitResult getTreatmentWithConfig(String key, String split, Map<String, Object> attributes) {
        return getTreatmentWithConfigInternal(GET_TREATMENT_WITH_CONFIG, key, null, split, attributes, MethodEnum.TREATMENT_WITH_CONFIG);
    }

    @Override
    public SplitResult getTreatmentWithConfig(Key key, String split, Map<String, Object> attributes) {
        return getTreatmentWithConfigInternal(GET_TREATMENT_WITH_CONFIG, key.matchingKey(), key.bucketingKey(), split, attributes, MethodEnum.TREATMENT_WITH_CONFIG);
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
        event.properties = new HashMap<>(properties);
        return track(event);
    }

    @Override
    public boolean track(String key, String trafficType, String eventType, double value, Map<String, Object> properties) {
        Event event = createEvent(key, trafficType, eventType);
        event.properties = new HashMap<>(properties);
        event.value = value;
        return track(event);
    }

    @Override
    public void blockUntilReady() throws TimeoutException, InterruptedException {
        long startTime = System.currentTimeMillis();
        if (_config.blockUntilReady() <= 0) {
            throw new IllegalArgumentException("setBlockUntilReadyTimeout must be positive but in config was: " + _config.blockUntilReady());
        }
        if (!_gates.waitUntilInternalReady(_config.blockUntilReady())) {
            throw new TimeoutException("SDK was not ready in " + _config.blockUntilReady()+ " milliseconds");
        }
        _log.debug(String.format("Split SDK ready in %d ms", (System.currentTimeMillis() - startTime)));
    }

    @Override
    public void destroy() {
        _container.destroy();
    }

    private boolean track(Event event) {
        long initTime = System.currentTimeMillis();
        if (_container.isDestroyed()) {
            _log.error("Client has already been destroyed - no calls possible");
            return false;
        }

        // Traffic Type validations
        Optional<String> trafficTypeResult = TrafficTypeValidator.isValid(event.trafficTypeName, _splitCache, "track");
        if (!trafficTypeResult.isPresent()) {
            return false;
        }
        event.trafficTypeName = trafficTypeResult.get();

        // EventType validations
        if (!EventsValidator.typeIsValid(event.eventTypeId, "track")) {
            return false;
        }

        // Key Validations
        if (!KeyValidator.isValid(event.key, "key", _config.maxStringLength(), "track")) {
            return false;
        }

        // Properties validations
        EventsValidator.EventValidatorResult propertiesResult = EventsValidator.propertiesAreValid(event.properties);
        if (!propertiesResult.getSuccess()) {
            return false;
        }

        event.properties = propertiesResult.getValue();
        _telemetryEvaluationProducer.recordLatency(MethodEnum.TRACK, System.currentTimeMillis() - initTime);

        return _eventClient.track(event, propertiesResult.getEventSize());
    }

    private SplitResult getTreatmentWithConfigInternal(String method, String matchingKey, String bucketingKey, String split, Map<String, Object> attributes, MethodEnum methodEnum) {
        long initTime = System.currentTimeMillis();
        try {
            if(!_gates.isSDKReady()){
                _log.warn(method + ": the SDK is not ready, results may be incorrect. Make sure to wait for SDK readiness before using this method");
                _telemetryConfigProducer.recordNonReadyUsage();
            }
            if (_container.isDestroyed()) {
                _log.error("Client has already been destroyed - no calls possible");
                return SPLIT_RESULT_CONTROL;
            }

            if (!KeyValidator.isValid(matchingKey, "matchingKey", _config.maxStringLength(), method)) {
                return SPLIT_RESULT_CONTROL;
            }

            if (!KeyValidator.bucketingKeyIsValid(bucketingKey, _config.maxStringLength(), method)) {
                return SPLIT_RESULT_CONTROL;
            }

            Optional<String> splitNameResult = SplitNameValidator.isValid(split, method);
            if (!splitNameResult.isPresent()) {
                return SPLIT_RESULT_CONTROL;
            }
            split = splitNameResult.get();

            long start = System.currentTimeMillis();

            EvaluatorImp.TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(matchingKey, bucketingKey, split, attributes);

            if (result.treatment.equals(Treatments.CONTROL) && result.label.equals(Labels.DEFINITION_NOT_FOUND) && _gates.isSDKReady()) {
                _log.warn(
                        "getTreatment: you passed \"" + split + "\" that does not exist in this environment, " +
                                "please double check what Splits exist in the web console.");
            }

            recordStats(
                    matchingKey,
                    bucketingKey,
                    split,
                    start,
                    result.treatment,
                    String.format("sdk.%s", method),
                    _config.labelsEnabled() ? result.label : null,
                    result.changeNumber,
                    attributes
            );
            _telemetryEvaluationProducer.recordLatency(methodEnum, System.currentTimeMillis()-initTime);
            return new SplitResult(result.treatment, result.configurations);
        } catch (Exception e) {
            try {
                _telemetryEvaluationProducer.recordException(methodEnum);
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
            _impressionManager.track(new Impression(matchingKey, bucketingKey, split, result, System.currentTimeMillis(), label, changeNumber, attributes));
        } catch (Throwable t) {
            _log.error("Exception", t);
        }
    }

    private Event createEvent(String key, String trafficType, String eventType) {
        Event event = new Event();
        event.eventTypeId = eventType;
        event.trafficTypeName = trafficType;
        event.key = key;
        event.timestamp = System.currentTimeMillis();
        return event;
    }
}
