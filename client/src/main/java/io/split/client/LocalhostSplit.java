package io.split.client;

public class LocalhostSplit {
    public String treatment;
    public String config;

    public LocalhostSplit(String treatment, String config) {
        this.treatment = treatment;
        this.config = config;
    }

    public static LocalhostSplit of(String treatment) {
        return new LocalhostSplit(treatment, null);
    }

    public static LocalhostSplit of(String treatment, String config) {
        return new LocalhostSplit(treatment, config);
    }

}


