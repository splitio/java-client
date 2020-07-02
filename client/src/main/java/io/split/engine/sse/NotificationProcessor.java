package io.split.engine.sse;

import io.split.engine.sse.dtos.IncomingNotification;

public interface NotificationProcessor {
    void process(IncomingNotification notification);
    void processSplitUpdate(long changeNumber);
    void processSplitKill(long changeNumber, String splitName, String defaultTreatment);
    void processSegmentUpdate(long changeNumber, String segmentName);
}
