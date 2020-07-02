package io.split.engine.sse.dtos;

import io.split.engine.sse.NotificationProcessor;

public class SplitChangeNotification extends IncomingNotification {
    private final long changeNumber;

    public SplitChangeNotification(GenericNotificationData genericNotificationData) {
        super(Type.SPLIT_UPDATE, genericNotificationData.getChannel());
        this.changeNumber = genericNotificationData.getChangeNumber();
    }

    public long getChangeNumber() {
        return changeNumber;
    }

    @Override
    public void handler(NotificationProcessor notificationProcessor) {
        notificationProcessor.processSplitUpdate(getChangeNumber());
    }
}
