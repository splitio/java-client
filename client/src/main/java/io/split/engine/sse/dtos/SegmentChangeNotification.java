package io.split.engine.sse.dtos;

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
}
