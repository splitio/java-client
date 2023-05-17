package io.split.engine.sse.dtos;

import io.split.engine.sse.NotificationProcessor;
import io.split.engine.sse.enums.CompressType;

public class FeatureFlagChangeNotification extends IncomingNotification {
    private final long changeNumber;
    private final long pcn;
    private final String d;
    private CompressType c;

    public FeatureFlagChangeNotification(GenericNotificationData genericNotificationData) {
        super(Type.SPLIT_UPDATE, genericNotificationData.getChannel());
        this.changeNumber = genericNotificationData.getChangeNumber();
        pcn = genericNotificationData.getPcn();
        d = genericNotificationData.getD();
        c = genericNotificationData.getC();
    }

    public long getChangeNumber() {
        return changeNumber;
    }

    @Override
    public void handler(NotificationProcessor notificationProcessor) {
        notificationProcessor.processSplitUpdate(getChangeNumber());
    }

    @Override
    public String toString() {
        return String.format("Type: %s; Channel: %s; ChangeNumber: %s", getType(), getChannel(), getChangeNumber());
    }
}