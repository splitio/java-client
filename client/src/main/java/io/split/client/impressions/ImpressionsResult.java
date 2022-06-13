package io.split.client.impressions;

import java.util.List;

public class ImpressionsResult {

    private List<Impression> impressionsToListener;
    private List<Impression> impressionsToSend;

    public ImpressionsResult(List<Impression> impressionsToListener, List<Impression> impressionsToSend) {
        this.impressionsToListener = impressionsToListener;
        this.impressionsToSend=impressionsToSend;
    }

    public List<Impression> getImpressionsToSend() {
        return impressionsToSend;
    }

    public List<Impression> getImpressionsToListener() {
        return impressionsToListener;
    }
}
