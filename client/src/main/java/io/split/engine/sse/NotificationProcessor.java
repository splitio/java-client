package io.split.engine.sse;

import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.dtos.SplitKillNotification;
import io.split.engine.sse.dtos.StatusNotification;

public interface NotificationProcessor {
    void process(IncomingNotification notification);
    void processUpdates(IncomingNotification notification);
    void processSplitKill(SplitKillNotification splitKillNotification);
    void processSegmentUpdate(long changeNumber, String segmentName);
    void processStatus(StatusNotification statusNotification);
}