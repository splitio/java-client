package io.split.engine.evaluator;

import io.split.client.SplitClient;
import io.split.client.SplitClientImpl;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.TreatmentLabelAndChangeNumber;
import io.split.client.exceptions.ChangeNumberExceptionWrapper;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.splitter.Splitter;
import io.split.grammar.Treatments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class EvaluatorImp implements Evaluator {
    private static final String NOT_IN_SPLIT = "not in split";
    private static final String DEFAULT_RULE = "default rule";
    private static final String KILLED = "killed";
    private static final String DEFINITION_NOT_FOUND = "definition not found";

    private static final Logger _log = LoggerFactory.getLogger(EvaluatorImp.class);

    private final SDKReadinessGates _gates;
    private final SplitFetcher _splitFetcher;

    public EvaluatorImp(SDKReadinessGates gates,
                        SplitFetcher splitFetcher) {
        _gates = gates;
        _splitFetcher = splitFetcher;
    }

    @Override
    public TreatmentLabelAndChangeNumber evaluateFeature(String matchingKey, String bucketingKey, String split, Map<String, Object> attributes, SplitClient splitClient) throws ChangeNumberExceptionWrapper {
        ParsedSplit parsedSplit = _splitFetcher.fetch(split);

        if (parsedSplit == null) {
            if (_gates.isSDKReadyNow()) {
                _log.warn(
                        "getTreatment: you passed \"" + split + "\" that does not exist in this environment, " +
                                "please double check what Splits exist in the web console.");
            }
            return new TreatmentLabelAndChangeNumber(Treatments.CONTROL, DEFINITION_NOT_FOUND);
        }

        return getTreatment(matchingKey, bucketingKey, parsedSplit, attributes, (SplitClientImpl)splitClient);
    }

    /**
     * @param matchingKey  MUST NOT be null
     * @param bucketingKey
     * @param parsedSplit  MUST NOT be null
     * @param attributes   MUST NOT be null
     * @return
     * @throws ChangeNumberExceptionWrapper
     */
    private TreatmentLabelAndChangeNumber getTreatment(String matchingKey, String bucketingKey, ParsedSplit parsedSplit, Map<String, Object> attributes, SplitClientImpl splitClient) throws ChangeNumberExceptionWrapper {
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

                if (parsedCondition.matcher().match(matchingKey, bucketingKey, attributes, splitClient)) {
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
}
