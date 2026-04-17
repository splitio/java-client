package io.split.rules.model;

public final class Partition {
    public final String treatment;
    public final int size;

    public Partition(String treatment, int size) {
        this.treatment = treatment;
        this.size = size;
    }
}
