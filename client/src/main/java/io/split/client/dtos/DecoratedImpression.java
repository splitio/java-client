package io.split.client.dtos;

import io.split.client.impressions.Impression;

public class DecoratedImpression {
    public Impression impression;
    public boolean track;

    public DecoratedImpression(Impression impression, boolean track) {
        this.impression = impression;
        this.track = track;
    }
}

