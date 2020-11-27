package io.split.client.dtos;

public final class TreatmentLabelAndChangeNumber {
    public final String treatment;
    public final String label;
    public final Long changeNumber;
    public final String configurations;

    public TreatmentLabelAndChangeNumber(String treatment, String label) {
        this(treatment, label, null, null);
    }

    public TreatmentLabelAndChangeNumber(String treatment, String label, Long changeNumber) {
        this(treatment, label, changeNumber, null);
    }

    public TreatmentLabelAndChangeNumber(String treatment, String label, Long changeNumber, String configurations) {
        this.treatment = treatment;
        this.label = label;
        this.changeNumber = changeNumber;
        this.configurations = configurations;
    }
}
