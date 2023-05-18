package io.split.engine.sse.dtos;

import io.split.engine.sse.NotificationProcessor;
import io.split.engine.sse.enums.CompressType;

public class FeatureFlagChangeNotification extends IncomingNotification {
    private final long changeNumber;
    private long previousChangeNumber;
    private String featureFlagDefinition;
    private CompressType compressType;


    public FeatureFlagChangeNotification(GenericNotificationData genericNotificationData) {
        super(Type.SPLIT_UPDATE, genericNotificationData.getChannel());
        changeNumber = genericNotificationData.getChangeNumber();
        if(genericNotificationData.getPreviousChangeNumber() != null) {
            previousChangeNumber = genericNotificationData.getPreviousChangeNumber();
        }
        featureFlagDefinition = genericNotificationData.getFeatureFlagDefinition();
        compressType =  CompressType.from(genericNotificationData.getCompressType());
    }

    public long getChangeNumber() {
        return changeNumber;
    }
    public long getPreviousChangeNumber() {
        return previousChangeNumber;
    }

    public String getFeatureFlagDefinition() {
        return featureFlagDefinition;
    }

    public CompressType getCompressType() {
        return compressType;
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