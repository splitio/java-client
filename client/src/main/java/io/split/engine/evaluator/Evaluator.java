package io.split.engine.evaluator;

import io.split.client.SplitClient;
import io.split.client.dtos.TreatmentLabelAndChangeNumber;
import io.split.client.exceptions.ChangeNumberExceptionWrapper;

import java.util.Map;

public interface Evaluator {
    TreatmentLabelAndChangeNumber evaluateFeature(String matchingKey, String bucketingKey, String split, Map<String, Object> attributes, SplitClient splitClient) throws ChangeNumberExceptionWrapper;
}
