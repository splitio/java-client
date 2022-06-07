package io.split.client.impressions.strategy;

import io.split.client.impressions.ImpressionsManager;

public class ProcessImpressionFactory {

    public ProcessImpressionStrategy createProcessImpression(ImpressionsManager.Mode impressionMode){
        if (impressionMode == ImpressionsManager.Mode.OPTIMIZED){
            return new ProcessImpressionOptimized();
        }
        if (impressionMode == ImpressionsManager.Mode.DEBUG){
            return new ProcessImpressionDebug();
        }
        if (impressionMode == ImpressionsManager.Mode.NONE){
            return new ProcessImpressionMTK();
        }
        return null;
    }
}
