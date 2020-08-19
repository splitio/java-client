package io.split.engine.sse.dtos;

import io.split.engine.sse.PushStatusTracker;
import io.split.engine.sse.NotificationProcessor;

public class ControlNotification extends IncomingNotification implements StatusNotification {
    private final ControlType controlType;

    public ControlNotification(GenericNotificationData genericNotificationData) {
        super(Type.CONTROL, genericNotificationData.getChannel());
        this.controlType = genericNotificationData.getControlType();
    }

    public ControlType getControlType() {
        return controlType;
    }

    @Override
    public void handler(NotificationProcessor notificationProcessor) {
        notificationProcessor.processStatus(this);
    }

    @Override
    public void handlerStatus(PushStatusTracker pushStatusTracker) {
        pushStatusTracker.handleIncomingControlEvent(this);
    }

    @Override
    public String toString() {
        return String.format("Type: %s; Channel: %s; ControlType: %s", getType(), getChannel(), getControlType());
    }
}
