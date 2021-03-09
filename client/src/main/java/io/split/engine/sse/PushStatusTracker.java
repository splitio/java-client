package io.split.engine.sse;

import io.split.engine.sse.client.SSEClient;
import io.split.engine.sse.dtos.ControlNotification;
import io.split.engine.sse.dtos.ErrorNotification;
import io.split.engine.sse.dtos.OccupancyNotification;

public interface PushStatusTracker {
    void handleIncomingControlEvent(ControlNotification controlNotification);
    void handleIncomingOccupancyEvent(OccupancyNotification occupancyNotification);
    void handleIncomingAblyError(ErrorNotification notification);
    void handleSseStatus(SSEClient.StatusMessage newStatus);
    void forcePushDisable();
    void notifyStreamingReady();
    void forceRetryableError();
}
