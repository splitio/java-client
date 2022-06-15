package io.split.client.impressions;

import java.util.List;

public class ImpressionsResult {

    private List<Impression> impressionsToListener;
    private List<Impression> impressionsForLogs;

    public ImpressionsResult(List<Impression> impressionsForLogs, List<Impression> impressionsToListener) {
        this.impressionsToListener = impressionsToListener;
        this.impressionsForLogs = impressionsForLogs;
    }

    public List<Impression> getImpressionsForLogs() {
        return impressionsForLogs;
    }

    public List<Impression> getImpressionsToListener() {
        return impressionsToListener;
    }
}
