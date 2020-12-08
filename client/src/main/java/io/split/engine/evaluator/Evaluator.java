package io.split.engine.evaluator;

import java.util.Map;

public interface Evaluator {
    EvaluatorImp.TreatmentLabelAndChangeNumber evaluateFeature(String matchingKey, String bucketingKey, String split, Map<String, Object> attributes);
}
