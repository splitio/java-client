package io.split.client.dtos;

import io.split.client.impressions.Impression;

public class DecoratedImpression {
    private Impression impression;
    private boolean disabled;

    public DecoratedImpression(Impression impression, boolean disabled) {
        this.impression = impression;
        this.disabled = disabled;
    }

    public Impression impression() { return this.impression;}

    public boolean disabled() { return this.disabled;}
}

