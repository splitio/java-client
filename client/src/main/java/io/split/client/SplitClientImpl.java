package io.split.client;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import io.split.client.api.Key;
import io.split.client.api.SplitResult;
import io.split.client.dtos.DecoratedImpression;
import io.split.client.dtos.Event;
import io.split.client.events.EventsStorageProducer;
import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionsManager;
import io.split.client.interceptors.FlagSetsFilter;
import io.split.engine.SDKReadinessGates;
import io.split.engine.evaluator.Evaluator;
import io.split.engine.evaluator.EvaluatorImp;
import io.split.engine.evaluator.Labels;
import io.split.grammar.Treatments;
import io.split.inputValidation.EventsValidator;
import io.split.inputValidation.KeyValidator;
import io.split.inputValidation.SplitNameValidator;
import io.split.inputValidation.TrafficTypeValidator;
import io.split.inputValidation.ImpressionPropertiesValidator;
import io.split.storages.SplitCacheConsumer;
import io.split.telemetry.domain.enums.MethodEnum;
import io.split.telemetry.storage.TelemetryConfigProducer;
import io.split.telemetry.storage.TelemetryEvaluationProducer;
import io.split.client.utils.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.split.inputValidation.FlagSetsValidator.cleanup;

/**
 * A basic implementation of SplitClient.
 *
 * @author adil
 */
public final class SplitClientImpl implements SplitClient {
    public static final SplitResult SPLIT_RESULT_CONTROL = new SplitResult(Treatments.CONTROL, null);
    private static final String CLIENT_DESTROY = "Client has already been destroyed - no calls possible";
    private static final String CATCHALL_EXCEPTION = "CatchAll Exception";
    private static final String MATCHING_KEY = "matchingKey";

    private static final Logger _log = LoggerFactory.getLogger(SplitClientImpl.class);

    private final SplitFactory _container;
    private final SplitCacheConsumer _splitCacheConsumer;
    private final ImpressionsManager _impressionManager;
    private final SplitClientConfig _config;
    private final EventsStorageProducer _eventsStorageProducer;
    private final SDKReadinessGates _gates;
    private final Evaluator _evaluator;
    private final TelemetryEvaluationProducer _telemetryEvaluationProducer;
    private final TelemetryConfigProducer _telemetryConfigProducer;
    private final FlagSetsFilter _flagSetsFilter;

    public SplitClientImpl(SplitFactory container,
                           SplitCacheConsumer splitCacheConsumer,
                           ImpressionsManager impressionManager,
                           EventsStorageProducer eventsStorageProducer,
                           SplitClientConfig config,
                           SDKReadinessGates gates,
                           Evaluator evaluator,
                           TelemetryEvaluationProducer telemetryEvaluationProducer,
                           TelemetryConfigProducer telemetryConfigProducer,
                           FlagSetsFilter flagSetsFilter) {
        _container = container;
        _splitCacheConsumer = checkNotNull(splitCacheConsumer);
        _impressionManager = checkNotNull(impressionManager);
        _eventsStorageProducer = eventsStorageProducer;
        _config = config;
        _gates = checkNotNull(gates);
        _evaluator = checkNotNull(evaluator);
        _telemetryEvaluationProducer = checkNotNull(telemetryEvaluationProducer);
        _telemetryConfigProducer = checkNotNull(telemetryConfigProducer);
        _flagSetsFilter = flagSetsFilter;
    }

    @Override
    public String getTreatment(String key, String featureFlagName) {
        return getTreatment(key, featureFlagName, Collections.<String, Object>emptyMap());
    }

    @Override
    public String getTreatment(String key, String featureFlagName, Map<String, Object> attributes) {
        return getTreatmentWithConfigInternal(key, null, featureFlagName, attributes, null, MethodEnum.TREATMENT).treatment();
    }

    @Override
    public String getTreatment(Key key, String featureFlagName, Map<String, Object> attributes) {
        return getTreatmentWithConfigInternal(key.matchingKey(), key.bucketingKey(), featureFlagName, attributes, null,
                MethodEnum.TREATMENT).treatment();
    }

    @Override
    public SplitResult getTreatmentWithConfig(String key, String featureFlagName) {
        return getTreatmentWithConfigInternal(key, null, featureFlagName, Collections.<String, Object>emptyMap(), null,
                MethodEnum.TREATMENT_WITH_CONFIG);
    }

    @Override
    public SplitResult getTreatmentWithConfig(String key, String featureFlagName, Map<String, Object> attributes) {
        return getTreatmentWithConfigInternal(key, null, featureFlagName, attributes, null, MethodEnum.TREATMENT_WITH_CONFIG);
    }

    @Override
    public SplitResult getTreatmentWithConfig(Key key, String featureFlagName, Map<String, Object> attributes) {
        return getTreatmentWithConfigInternal(key.matchingKey(), key.bucketingKey(), featureFlagName, attributes, null,
                MethodEnum.TREATMENT_WITH_CONFIG);
    }

    @Override
    public Map<String, String> getTreatments(String key, List<String> featureFlagNames) {
        return getTreatments(key, featureFlagNames, Collections.emptyMap());
    }

    @Override
    public Map<String, String> getTreatments(String key, List<String> featureFlagNames, Map<String, Object> attributes) {
        return getTreatmentsWithConfigInternal(key, null, featureFlagNames, attributes, null, MethodEnum.TREATMENTS)
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().treatment()));
    }

    @Override
    public Map<String, String> getTreatments(Key key, List<String> featureFlagNames, Map<String, Object> attributes) {
        return getTreatmentsWithConfigInternal(key.matchingKey(), key.bucketingKey(), featureFlagNames, attributes, null,  MethodEnum.TREATMENTS)
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().treatment()));
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(String key, List<String> featureFlagNames) {
        return getTreatmentsWithConfigInternal(key, null, featureFlagNames, Collections.<String, Object>emptyMap(), null,
                MethodEnum.TREATMENTS_WITH_CONFIG);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(String key, List<String> featureFlagNames, Map<String, Object> attributes) {
        return getTreatmentsWithConfigInternal(key, null, featureFlagNames, attributes, null, MethodEnum.TREATMENTS_WITH_CONFIG);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(Key key, List<String> featureFlagNames, Map<String, Object> attributes) {
        return getTreatmentsWithConfigInternal(key.matchingKey(), key.bucketingKey(), featureFlagNames, attributes, null,
                MethodEnum.TREATMENTS_WITH_CONFIG);
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSet(String key, String flagSet) {
        return getTreatmentsBySetsWithConfigInternal(key, null, new ArrayList<>(Arrays.asList(flagSet)),
                null, null, MethodEnum.TREATMENTS_BY_FLAG_SET).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().treatment()));
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSet(String key, String flagSet, Map<String, Object> attributes) {
        return getTreatmentsBySetsWithConfigInternal(key, null, new ArrayList<>(Arrays.asList(flagSet)),
                attributes, null, MethodEnum.TREATMENTS_BY_FLAG_SET).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().treatment()));
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSet(Key key, String flagSet, Map<String, Object> attributes) {
        return getTreatmentsBySetsWithConfigInternal(key.matchingKey(), key.bucketingKey(), new ArrayList<>(Arrays.asList(flagSet)),
                attributes, null, MethodEnum.TREATMENTS_BY_FLAG_SET).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().treatment()));
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSets(String key, List<String> flagSets) {
        return getTreatmentsBySetsWithConfigInternal(key, null, flagSets,
                null, null, MethodEnum.TREATMENTS_BY_FLAG_SETS).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().treatment()));
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSets(String key, List<String> flagSets, Map<String, Object> attributes) {
        return getTreatmentsBySetsWithConfigInternal(key, null, flagSets,
                attributes, null, MethodEnum.TREATMENTS_BY_FLAG_SETS).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().treatment()));
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSets(Key key, List<String> flagSets, Map<String, Object> attributes) {
        return getTreatmentsBySetsWithConfigInternal(key.matchingKey(), key.bucketingKey(), flagSets,
                attributes, null, MethodEnum.TREATMENTS_BY_FLAG_SETS).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().treatment()));
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(String key, String flagSet) {
        return getTreatmentsBySetsWithConfigInternal(key, null, new ArrayList<>(Arrays.asList(flagSet)),
                null, null, MethodEnum.TREATMENTS_WITH_CONFIG_BY_FLAG_SET);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(String key, String flagSet, Map<String, Object> attributes) {
        return getTreatmentsBySetsWithConfigInternal(key, null, new ArrayList<>(Arrays.asList(flagSet)),
                attributes, null, MethodEnum.TREATMENTS_WITH_CONFIG_BY_FLAG_SET);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(Key key, String flagSet, Map<String, Object> attributes) {
        return getTreatmentsBySetsWithConfigInternal(key.matchingKey(), key.bucketingKey(), new ArrayList<>(Arrays.asList(flagSet)),
                attributes, null, MethodEnum.TREATMENTS_WITH_CONFIG_BY_FLAG_SET);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(String key, List<String> flagSets) {
        return getTreatmentsBySetsWithConfigInternal(key, null, flagSets,
                null, null, MethodEnum.TREATMENTS_WITH_CONFIG_BY_FLAG_SETS);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(String key, List<String> flagSets, Map<String, Object> attributes) {
        return getTreatmentsBySetsWithConfigInternal(key, null, flagSets,
                attributes, null, MethodEnum.TREATMENTS_WITH_CONFIG_BY_FLAG_SETS);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(Key key, List<String> flagSets, Map<String, Object> attributes) {
        return getTreatmentsBySetsWithConfigInternal(key.matchingKey(), key.bucketingKey(), flagSets,
                attributes, null, MethodEnum.TREATMENTS_WITH_CONFIG_BY_FLAG_SETS);
    }

    @Override
    public String getTreatment(String key, String featureFlagName, String properties) {
        return getTreatment(key, featureFlagName, Collections.<String, Object>emptyMap(), properties);
    }

    @Override
    public String getTreatment(String key, String featureFlagName, Map<String, Object> attributes, String properties) {
        return getTreatmentWithConfigInternal(key, null, featureFlagName, attributes, properties, MethodEnum.TREATMENT).treatment();
    }

    @Override
    public String getTreatment(Key key, String featureFlagName, Map<String, Object> attributes, String properties) {
        return getTreatmentWithConfigInternal(key.matchingKey(), key.bucketingKey(), featureFlagName, attributes, properties,
                MethodEnum.TREATMENT).treatment();
    }

    @Override
    public Map<String, String> getTreatments(String key, List<String> featureFlagNames, String properties) {
        return getTreatments(key, featureFlagNames, Collections.emptyMap(), properties);
    }

    @Override
    public Map<String, String> getTreatments(String key, List<String> featureFlagNames, Map<String, Object> attributes, String properties) {
        return getTreatmentsWithConfigInternal(key, null, featureFlagNames, attributes, properties, MethodEnum.TREATMENTS)
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().treatment()));
    }

    @Override
    public Map<String, String> getTreatments(Key key, List<String> featureFlagNames, Map<String, Object> attributes, String properties) {
        return getTreatmentsWithConfigInternal(key.matchingKey(), key.bucketingKey(), featureFlagNames, attributes, properties,
                MethodEnum.TREATMENTS)
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().treatment()));
    }

    @Override
    public SplitResult getTreatmentWithConfig(String key, String featureFlagName, String properties) {
        return getTreatmentWithConfigInternal(key, null, featureFlagName, Collections.<String, Object>emptyMap(), properties,
                MethodEnum.TREATMENT_WITH_CONFIG);
    }

    @Override
    public SplitResult getTreatmentWithConfig(Key key, String featureFlagName, Map<String, Object> attributes, String properties) {
        return getTreatmentWithConfigInternal(key.matchingKey(), key.bucketingKey(), featureFlagName, attributes, properties,
                MethodEnum.TREATMENT_WITH_CONFIG);
    }

    @Override
    public SplitResult getTreatmentWithConfig(String key, String featureFlagName, Map<String, Object> attributes, String properties) {
        return getTreatmentWithConfigInternal(key, null, featureFlagName, attributes, properties,
                MethodEnum.TREATMENT_WITH_CONFIG);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(String key, List<String> featureFlagNames, Map<String, Object> attributes,
                                                            String properties) {
        return getTreatmentsWithConfigInternal(key, null, featureFlagNames, attributes, properties,
                MethodEnum.TREATMENTS_WITH_CONFIG);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(String key, List<String> featureFlagNames, String properties) {
        return getTreatmentsWithConfigInternal(key, null, featureFlagNames, null, properties,
                MethodEnum.TREATMENTS_WITH_CONFIG);
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSet(String key, String flagSet, Map<String, Object> attributes, String properties) {
        return getTreatmentsBySetsWithConfigInternal(key, null, new ArrayList<>(Arrays.asList(flagSet)),
                attributes, properties, MethodEnum.TREATMENTS_BY_FLAG_SET).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().treatment()));
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSets(String key, List<String> flagSets, String properties) {
        return getTreatmentsBySetsWithConfigInternal(key, null, flagSets,
                null, properties, MethodEnum.TREATMENTS_BY_FLAG_SETS).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().treatment()));
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSets(String key, List<String> flagSets, Map<String, Object> attributes, String properties) {
        return getTreatmentsBySetsWithConfigInternal(key, null, flagSets,
                attributes, properties, MethodEnum.TREATMENTS_BY_FLAG_SETS).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().treatment()));
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(String key, String flagSet, String properties) {
        return getTreatmentsBySetsWithConfigInternal(key, null, new ArrayList<>(Arrays.asList(flagSet)),
                null, properties, MethodEnum.TREATMENTS_WITH_CONFIG_BY_FLAG_SET);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(String key, String flagSet, Map<String, Object> attributes, String properties) {
        return getTreatmentsBySetsWithConfigInternal(key, null, new ArrayList<>(Arrays.asList(flagSet)),
                attributes, properties, MethodEnum.TREATMENTS_WITH_CONFIG_BY_FLAG_SET);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(String key, List<String> flagSets, String properties) {
        return getTreatmentsBySetsWithConfigInternal(key, null, flagSets,
                null, null, MethodEnum.TREATMENTS_WITH_CONFIG_BY_FLAG_SETS);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(String key, List<String> flagSets, Map<String, Object> attributes,
                                                                      String properties) {
        return getTreatmentsBySetsWithConfigInternal(key, null, flagSets,
                attributes, null, MethodEnum.TREATMENTS_WITH_CONFIG_BY_FLAG_SETS);
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSet(String key, String flagSet, String properties) {
        return getTreatmentsBySetsWithConfigInternal(key, null, new ArrayList<>(Arrays.asList(flagSet)),
                null, properties, MethodEnum.TREATMENTS_BY_FLAG_SET).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().treatment()));
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(Key key, List<String> featureFlagNames, Map<String, Object> attributes,
                                                            String properties) {
        return getTreatmentsWithConfigInternal(key.matchingKey(), key.bucketingKey(), featureFlagNames, attributes, properties,
                MethodEnum.TREATMENTS_WITH_CONFIG);
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSet(Key key, String flagSet, Map<String, Object> attributes, String properties) {
        return getTreatmentsBySetsWithConfigInternal(key.matchingKey(), key.bucketingKey(), new ArrayList<>(Arrays.asList(flagSet)),
                attributes, properties, MethodEnum.TREATMENTS_BY_FLAG_SET).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().treatment()));
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSets(Key key, List<String> flagSets, Map<String, Object> attributes, String properties) {
        return getTreatmentsBySetsWithConfigInternal(key.matchingKey(), key.bucketingKey(), flagSets,
                attributes, properties, MethodEnum.TREATMENTS_BY_FLAG_SETS).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().treatment()));
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(Key key, String flagSet, Map<String, Object> attributes, String properties) {
        return getTreatmentsBySetsWithConfigInternal(key.matchingKey(), key.bucketingKey(), new ArrayList<>(Arrays.asList(flagSet)),
                attributes, properties, MethodEnum.TREATMENTS_WITH_CONFIG_BY_FLAG_SET);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(Key key, List<String> flagSets, Map<String, Object> attributes,
                                                                      String properties) {
        return getTreatmentsBySetsWithConfigInternal(key.matchingKey(), key.bucketingKey(), flagSets,
                attributes, null, MethodEnum.TREATMENTS_WITH_CONFIG_BY_FLAG_SETS);
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
            throw new TimeoutException("SDK was not ready in " + _config.blockUntilReady() + " milliseconds");
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
            _log.error(CLIENT_DESTROY);
            return false;
        }

        // Traffic Type validations
        Optional<String> trafficTypeResult = TrafficTypeValidator.isValid(event.trafficTypeName, _splitCacheConsumer, "track");
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

        return _eventsStorageProducer.track(event, propertiesResult.getEventSize());
    }

    private SplitResult getTreatmentWithConfigInternal(String matchingKey, String bucketingKey, String featureFlag, Map<String,
                                                       Object> attributes, String properties, MethodEnum methodEnum) {
        long initTime = System.currentTimeMillis();
        try {
            checkSDKReady(methodEnum, Arrays.asList(featureFlag));

            if (_container.isDestroyed()) {
                _log.error(CLIENT_DESTROY);
                return SPLIT_RESULT_CONTROL;
            }

            if (!KeyValidator.isValid(matchingKey, MATCHING_KEY, _config.maxStringLength(), methodEnum.getMethod())) {
                return SPLIT_RESULT_CONTROL;
            }

            if (!KeyValidator.bucketingKeyIsValid(bucketingKey, _config.maxStringLength(), methodEnum.getMethod())) {
                return SPLIT_RESULT_CONTROL;
            }

            Optional<String> splitNameResult = SplitNameValidator.isValid(featureFlag, methodEnum.getMethod());
            if (!splitNameResult.isPresent()) {
                return SPLIT_RESULT_CONTROL;
            }
            featureFlag = splitNameResult.get();
            long start = System.currentTimeMillis();

            EvaluatorImp.TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(matchingKey, bucketingKey, featureFlag, attributes);

            if (result.treatment.equals(Treatments.CONTROL) && result.label.equals(Labels.DEFINITION_NOT_FOUND) && _gates.isSDKReady()) {
                _log.warn(String.format(
                        "%s: you passed \"%s\" that does not exist in this environment, " +
                                "please double check what feature flags exist in the Split user interface.", methodEnum.getMethod(), featureFlag));
                return SPLIT_RESULT_CONTROL;
            }

            recordStats(
                    matchingKey,
                    bucketingKey,
                    featureFlag,
                    start,
                    result.treatment,
                    String.format("sdk.%s", methodEnum.getMethod()),
                    _config.labelsEnabled() ? result.label : null,
                    result.changeNumber,
                    attributes,
                    result.track,
                    validateProperties(properties)
            );
            _telemetryEvaluationProducer.recordLatency(methodEnum, System.currentTimeMillis() - initTime);
            return new SplitResult(result.treatment, result.configurations);
        } catch (Exception e) {
            try {
                _telemetryEvaluationProducer.recordException(methodEnum);
                _log.error(CATCHALL_EXCEPTION, e);
            } catch (Exception e1) {
                // ignore
            }
            return SPLIT_RESULT_CONTROL;
        }
    }

    private String validateProperties(String properties) {
        String validatedProperties = null;
        if (properties != null) {
            ImpressionPropertiesValidator.ImpressionPropertiesValidatorResult iPValidatorResult = ImpressionPropertiesValidator.propertiesAreValid(
                    new JsonParser().parse(properties).getAsJsonObject());
            validatedProperties = iPValidatorResult.getValue().toString();
        }
        return validatedProperties;
    }

    private Map<String, SplitResult> getTreatmentsWithConfigInternal(String matchingKey, String bucketingKey, List<String> featureFlagNames,
                                                                     Map<String, Object> attributes, String properties, MethodEnum methodEnum) {
        long initTime = System.currentTimeMillis();
        if (featureFlagNames == null) {
            _log.error(String.format("%s: featureFlagNames must be a non-empty array", methodEnum.getMethod()));
            return new HashMap<>();
        }
        try {
            checkSDKReady(methodEnum, featureFlagNames);
            Map<String, SplitResult> result = validateBeforeEvaluate(featureFlagNames, matchingKey, methodEnum, bucketingKey);
            if(result != null) {
                return result;
            }
            featureFlagNames = SplitNameValidator.areValid(featureFlagNames, methodEnum.getMethod());
            Map<String, EvaluatorImp.TreatmentLabelAndChangeNumber> evaluatorResult = _evaluator.evaluateFeatures(matchingKey,
                    bucketingKey, featureFlagNames, attributes);

            return processEvaluatorResult(evaluatorResult, methodEnum, matchingKey, bucketingKey, attributes, initTime,
                    validateProperties(properties));
        } catch (Exception e) {
            try {
                _telemetryEvaluationProducer.recordException(methodEnum);
                _log.error(CATCHALL_EXCEPTION, e);
            } catch (Exception e1) {
                // ignore
            }
            return createMapControl(featureFlagNames);
        }
    }

    private Map<String, SplitResult> getTreatmentsBySetsWithConfigInternal(String matchingKey, String bucketingKey,
                                                                           List<String> sets, Map<String, Object> attributes, String properties,
                                                                           MethodEnum methodEnum) {

        long initTime = System.currentTimeMillis();
        if (sets == null || sets.isEmpty()) {
            _log.warn(String.format("%s: sets must be a non-empty array", methodEnum.getMethod()));
            return new HashMap<>();
        }
        Set cleanFlagSets = cleanup(sets);
        cleanFlagSets = filterSetsAreInConfig(cleanFlagSets, methodEnum);
        if (cleanFlagSets.isEmpty()) {
            return new HashMap<>();
        }
        List<String> featureFlagNames = new ArrayList<>();
        try {
            checkSDKReady(methodEnum);
            Map<String, SplitResult> result = validateBeforeEvaluateByFlagSets(matchingKey, methodEnum,bucketingKey);
            if(result != null) {
                return result;
            }
            Map<String, EvaluatorImp.TreatmentLabelAndChangeNumber> evaluatorResult = _evaluator.evaluateFeaturesByFlagSets(matchingKey,
                    bucketingKey, new ArrayList<>(cleanFlagSets), attributes);

            return processEvaluatorResult(evaluatorResult, methodEnum, matchingKey, bucketingKey, attributes, initTime,
                    validateProperties(properties));
        } catch (Exception e) {
            try {
                _telemetryEvaluationProducer.recordException(methodEnum);
                _log.error(CATCHALL_EXCEPTION, e);
            } catch (Exception e1) {
                // ignore
            }
            return createMapControl(featureFlagNames);
        }
    }
    private Map<String, SplitResult> processEvaluatorResult(Map<String, EvaluatorImp.TreatmentLabelAndChangeNumber> evaluatorResult,
                                                            MethodEnum methodEnum, String matchingKey, String bucketingKey, Map<String,
                                                            Object> attributes, long initTime, String properties){
        List<DecoratedImpression> decoratedImpressions = new ArrayList<>();
        Map<String, SplitResult> result = new HashMap<>();
        evaluatorResult.keySet().forEach(t -> {
            if (evaluatorResult.get(t).treatment.equals(Treatments.CONTROL) && evaluatorResult.get(t).label.
                    equals(Labels.DEFINITION_NOT_FOUND) && _gates.isSDKReady()) {
                _log.warn(String.format("%s: you passed \"%s\" that does not exist in this environment please double check " +
                        "what feature flags exist in the Split user interface.", methodEnum.getMethod(), t));
                result.put(t, SPLIT_RESULT_CONTROL);
            } else {
                result.put(t, new SplitResult(evaluatorResult.get(t).treatment, evaluatorResult.get(t).configurations));
                decoratedImpressions.add(
                        new DecoratedImpression(
                                new Impression(matchingKey, bucketingKey, t, evaluatorResult.get(t).treatment, System.currentTimeMillis(),
                        evaluatorResult.get(t).label, evaluatorResult.get(t).changeNumber, attributes, properties),
                                evaluatorResult.get(t).track));
            }
        });
        _telemetryEvaluationProducer.recordLatency(methodEnum, System.currentTimeMillis() - initTime);
        if (!decoratedImpressions.isEmpty()) {
            _impressionManager.track(decoratedImpressions);
        }
        return result;
    }

    private Map<String, SplitResult> validateBeforeEvaluateByFlagSets(String matchingKey, MethodEnum methodEnum,
                                                            String bucketingKey) {
        if (_container.isDestroyed()) {
            _log.error(CLIENT_DESTROY);
            return new HashMap<>();
        }
        if (!KeyValidator.isValid(matchingKey, MATCHING_KEY, _config.maxStringLength(), methodEnum.getMethod())) {
            return new HashMap<>();
        }
        if (!KeyValidator.bucketingKeyIsValid(bucketingKey, _config.maxStringLength(), methodEnum.getMethod())) {
            return new HashMap<>();
        }
        return null;
    }

    private Map<String, SplitResult> validateBeforeEvaluate(List<String> featureFlagNames, String matchingKey, MethodEnum methodEnum,
                                                            String bucketingKey) {
        if (_container.isDestroyed()) {
            _log.error(CLIENT_DESTROY);
            return createMapControl(featureFlagNames);
        }
        if (!KeyValidator.isValid(matchingKey, MATCHING_KEY, _config.maxStringLength(), methodEnum.getMethod())) {
            return createMapControl(featureFlagNames);
        }
        if (!KeyValidator.bucketingKeyIsValid(bucketingKey, _config.maxStringLength(), methodEnum.getMethod())) {
            return createMapControl(featureFlagNames);
        } else if (featureFlagNames.isEmpty()) {
            _log.error(String.format("%s: featureFlagNames must be a non-empty array", methodEnum.getMethod()));
            return new HashMap<>();
        }
        return null;
    }
    private Set<String> filterSetsAreInConfig(Set<String> sets, MethodEnum methodEnum) {
        Set<String> setsToReturn = new HashSet<>();
        for (String set : sets) {
            if (!_flagSetsFilter.intersect(set)) {
                _log.warn(String.format("%s: you passed %s which is not part of the configured FlagSetsFilter, " +
                        "ignoring Flag Set.", methodEnum, set));
                continue;
            }
            setsToReturn.add(set);
        }
        return setsToReturn;
    }
    private void recordStats(String matchingKey, String bucketingKey, String featureFlagName, long start, String result,
                             String operation, String label, Long changeNumber, Map<String, Object> attributes, boolean track, String properties) {
        try {
            _impressionManager.track(Stream.of(
                    new DecoratedImpression(
                            new Impression(matchingKey, bucketingKey, featureFlagName, result, System.currentTimeMillis(),
                    label, changeNumber, attributes, properties),
                            track)).collect(Collectors.toList()));
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

    private void checkSDKReady(MethodEnum methodEnum, List<String> featureFlagNames) {
        String toPrint =  featureFlagNames.size() == 1 ? featureFlagNames.get(0): String.join(",", featureFlagNames);
        if (!_gates.isSDKReady()) {
            _log.warn(String.format("%s: the SDK is not ready, results may be incorrect for feature flag %s. Make sure to wait for " +
                            "SDK readiness before using this method", methodEnum.getMethod(), toPrint));
            _telemetryConfigProducer.recordNonReadyUsage();
        }
    }

    private void checkSDKReady(MethodEnum methodEnum) {
        if (!_gates.isSDKReady()) {
            _log.warn(String.format("%s: the SDK is not ready, results may be incorrect. Make sure to wait for " +
                    "SDK readiness before using this method", methodEnum.getMethod()));
            _telemetryConfigProducer.recordNonReadyUsage();
        }
    }

    private Map<String, SplitResult> createMapControl(List<String> featureFlags) {
        Map<String, SplitResult> result = new HashMap<>();
        featureFlags.forEach(s -> result.put(s, SPLIT_RESULT_CONTROL));
        return result;
    }
}