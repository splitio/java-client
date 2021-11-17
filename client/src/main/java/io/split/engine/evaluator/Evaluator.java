package io.split.engine.evaluator;

import java.util.List;
import java.util.Map;

public interface Evaluator {
    EvaluatorImp.TreatmentLabelAndChangeNumber evaluateFeature(String matchingKey, String bucketingKey, String split, Map<String, Object> attributes);
    Map<String, EvaluatorImp.TreatmentLabelAndChangeNumber> evaluateFeatures(String matchingKey, String bucketingKey, List<String> splits, Map<String, Object> attributes);
}
