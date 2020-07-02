package io.split.engine.sse.dtos;

import io.split.engine.sse.NotificationProcessor;

public class SplitKillNotification extends IncomingNotification {
    private final long changeNumber;
    private final String defaultTreatment;
    private final String splitName;

    public  SplitKillNotification(GenericNotificationData genericNotificationData) {
        super(Type.SPLIT_KILL, genericNotificationData.getChannel());
        this.changeNumber = genericNotificationData.getChangeNumber();
        this.defaultTreatment = genericNotificationData.getDefaultTreatment();
        this.splitName = genericNotificationData.getSplitName();
    }

    public long getChangeNumber() {
        return changeNumber;
    }

    public String getDefaultTreatment() {
        return defaultTreatment;
    }

    public String getSplitName() {
        return splitName;
    }

    @Override
    public void handler(NotificationProcessor notificationProcessor) {
        notificationProcessor.processSplitKill(getChangeNumber(), getSplitName(), getDefaultTreatment());
    }
}
