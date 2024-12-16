package io.split.client.dtos;

import io.split.client.impressions.Impression;

public class DecoratedImpression {
    private Impression impression;
    private boolean track;

    public DecoratedImpression(Impression impression, boolean track) {
        this.impression = impression;
        this.track = track;
    }

    public Impression impression() { return this.impression;}

    public boolean track() { return this.track;}
}

