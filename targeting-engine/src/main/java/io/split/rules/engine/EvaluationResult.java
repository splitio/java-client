package io.split.rules.engine;

public final class EvaluationResult {
    public final String treatment;
    public final String label;
    public final Long version;
    public final String config;
    public final boolean impressionsDisabled;

    public EvaluationResult(String treatment, String label) {
        this(treatment, label, null, null, false);
    }

    public EvaluationResult(String treatment, String label, Long version) {
        this(treatment, label, version, null, false);
    }

    public EvaluationResult(String treatment, String label, Long version, String config, boolean impressionsDisabled) {
        this.treatment = treatment;
        this.label = label;
        this.version = version;
        this.config = config;
        this.impressionsDisabled = impressionsDisabled;
    }
}
