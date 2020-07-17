package io.split.engine.sse;

import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.dtos.StatusNotification;

public interface NotificationProcessor {
    void process(IncomingNotification notification);
    void processSplitUpdate(long changeNumber);
    void processSplitKill(long changeNumber, String splitName, String defaultTreatment);
    void processSegmentUpdate(long changeNumber, String segmentName);
    void processStatus(StatusNotification statusNotification);
}