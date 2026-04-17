package io.split.rules.engine;

public final class EvaluationResult {
    public final String treatment;
    public final String label;

    public EvaluationResult(String treatment, String label) {
        this.treatment = treatment;
        this.label = label;
    }
}
