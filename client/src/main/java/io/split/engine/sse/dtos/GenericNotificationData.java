package io.split.engine.sse.dtos;

public class GenericNotificationData {
    private long changeNumber;
    private String defaultTreatment;
    private String splitName;
    private ControlType controlType;
    private OccupancyMetrics metrics;
    private String segmentName;
    private IncomingNotification.Type type;
    private String channel;

    public long getChangeNumber() {
        return changeNumber;
    }

    public String getDefaultTreatment() {
        return defaultTreatment;
    }

    public String getSplitName() {
        return splitName;
    }

    public ControlType getControlType() {
        return controlType;
    }

    public OccupancyMetrics getMetrics() {
        return metrics;
    }

    public String getSegmentName() {
        return segmentName;
    }

    public IncomingNotification.Type getType() {
        return type;
    }

    public String getChannel() { return channel; }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}
