package io.split.client.impressions;

import java.util.List;

public class ImpressionsResult {

    private List<Impression> impressionsToListener;
    private List<Impression> impressionsToQueue;

    public ImpressionsResult(List<Impression> impressionsForLogs, List<Impression> impressionsToListener) {
        this.impressionsToListener = impressionsToListener;
        this.impressionsToQueue = impressionsForLogs;
    }

    public List<Impression> getImpressionsToQueue() {
        return impressionsToQueue;
    }

    public List<Impression> getImpressionsToListener() {
        return impressionsToListener;
    }
}
