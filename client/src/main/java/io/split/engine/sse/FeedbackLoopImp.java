package io.split.engine.sse;

import io.split.engine.sse.dtos.ErrorNotification;
import io.split.engine.sse.dtos.IncomingNotification;

public class FeedbackLoopImp implements FeedbackLoopListener {
    @Override
    public void onMessageNotificationAdded(IncomingNotification incomingNotification) {
        // TODO: implement this.
    }

    @Override
    public void onErrorNotificationAdded(ErrorNotification errorNotification) {
        // TODO: implement this.
    }

    @Override
    public void onConnected() {
        // TODO: implement this.
    }

    @Override
    public void onDisconnect() {
        // TODO: implement this.
    }
}
