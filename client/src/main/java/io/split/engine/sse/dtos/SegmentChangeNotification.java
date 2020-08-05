package io.split.engine.sse.dtos;

import io.split.engine.sse.NotificationProcessor;

public class SegmentChangeNotification extends IncomingNotification {
    private final long changeNumber;
    private final String segmentName;

    public SegmentChangeNotification(GenericNotificationData genericNotificationData) {
        super(Type.SEGMENT_UPDATE, genericNotificationData.getChannel());
        this.changeNumber = genericNotificationData.getChangeNumber();
        this.segmentName = genericNotificationData.getSegmentName();
    }

    public String getSegmentName() {
        return segmentName;
    }

    public long getChangeNumber() {
        return changeNumber;
    }

    @Override
    public void handler(NotificationProcessor notificationProcessor) {
        notificationProcessor.processSegmentUpdate(getChangeNumber(), getSegmentName());
    }

    @Override
    public String toString() {
        return String.format("Type: %s; Channel: %s; ChangeNumber: %s; SegmentName: %s", getType(), getChannel(), getChangeNumber(), getSegmentName());
    }
}
