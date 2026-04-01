package io.split.rules.engine;

import io.split.rules.bucketing.Bucketer;
import io.split.rules.exceptions.VersionedExceptionWrapper;
import io.split.rules.model.Condition;
import io.split.rules.model.ConditionType;
import io.split.rules.model.TargetingRule;

import java.util.Map;

public final class TargetingEngineImpl implements TargetingEngine {

    @Override
    public EvaluationResult evaluate(String matchingKey, String bucketingKey,
                                     TargetingRule rule, Map<String, Object> attributes,
                                     EvaluationContext context) throws VersionedExceptionWrapper {
        try {
            String config = getConfig(rule, rule.defaultTreatment());

            // 1. Killed rule → return default treatment
            if (rule.killed()) {
                return new EvaluationResult(rule.defaultTreatment(), EvaluationLabels.KILLED,
                        rule.changeNumber(), config, rule.impressionsDisabled());
            }

            // 2. Bucketing key resolution
            String bk = bucketingKey != null ? bucketingKey : matchingKey;

            // 3. Prerequisites check
            if (!rule.prerequisitesMatcher().match(matchingKey, bk, attributes, context)) {
                return new EvaluationResult(rule.defaultTreatment(), EvaluationLabels.PREREQUISITES_NOT_MET,
                        rule.changeNumber(), config, rule.impressionsDisabled());
            }

            // 4. Iterate conditions
            boolean inRollout = false;
            for (Condition condition : rule.conditions()) {

                // 4a. Traffic allocation check (once, before first ROLLOUT condition)
                if (!inRollout && condition.conditionType() == ConditionType.ROLLOUT) {
                    if (rule.trafficAllocation() < 100) {
                        int bucket = Bucketer.getBucket(bk, rule.trafficAllocationSeed(), rule.algo());
                        if (bucket > rule.trafficAllocation()) {
                            config = getConfig(rule, rule.defaultTreatment());
                            return new EvaluationResult(rule.defaultTreatment(), EvaluationLabels.NOT_IN_SPLIT,
                                    rule.changeNumber(), config, rule.impressionsDisabled());
                        }
                    }
                    inRollout = true;
                }

                // 4b. Condition match → select treatment
                if (condition.matcher().match(matchingKey, bucketingKey, attributes, context)) {
                    String treatment = Bucketer.getTreatment(bk, rule.seed(), condition.partitions(), rule.algo());
                    config = getConfig(rule, treatment);
                    return new EvaluationResult(treatment, condition.label(),
                            rule.changeNumber(), config, rule.impressionsDisabled());
                }
            }

            // 5. No condition matched → default rule
            config = getConfig(rule, rule.defaultTreatment());
            return new EvaluationResult(rule.defaultTreatment(), EvaluationLabels.DEFAULT_RULE,
                    rule.changeNumber(), config, rule.impressionsDisabled());

        } catch (Exception e) {
            throw new VersionedExceptionWrapper(e, rule.changeNumber());
        }
    }

    private String getConfig(TargetingRule rule, String treatment) {
        return rule.configurations() != null ? rule.configurations().get(treatment) : null;
    }
}
