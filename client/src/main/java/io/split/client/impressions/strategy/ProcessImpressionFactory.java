package io.split.client.impressions.strategy;

import io.split.client.impressions.ImpressionCounter;
import io.split.client.impressions.ImpressionObserver;
import io.split.client.impressions.ImpressionsManager;
import io.split.client.impressions.UniqueKeysTracker;

public class ProcessImpressionFactory {

    public ProcessImpressionStrategy createProcessImpression(ImpressionsManager.Mode impressionMode, UniqueKeysTracker uniqueKeysTracker, ImpressionObserver impressionObserver, ImpressionCounter impressionCounter){
        if (impressionMode == ImpressionsManager.Mode.OPTIMIZED){
            return new ProcessImpressionOptimized(impressionObserver,impressionCounter);
        }
        if (impressionMode == ImpressionsManager.Mode.DEBUG){
            return new ProcessImpressionDebug(impressionObserver);
        }
        if (impressionMode == ImpressionsManager.Mode.NONE){
            return new ProcessImpressionNone(uniqueKeysTracker, impressionCounter);
        }
        return null;
    }
}
