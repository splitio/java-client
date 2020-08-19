package io.split.engine.sse.dtos;

public class GenericNotificationData {
    private final Long changeNumber;
    private final String defaultTreatment;
    private final String splitName;
    private final ControlType controlType;
    private final OccupancyMetrics metrics;
    private final String segmentName;
    private final IncomingNotification.Type type;
    private String channel;

    public GenericNotificationData (Long changeNumber,
                                    String defaultTreatment,
                                    String splitName,
                                    ControlType controlType,
                                    OccupancyMetrics occupancyMetrics,
                                    String segmentName,
                                    IncomingNotification.Type type,
                                    String channel) {
        this.changeNumber = changeNumber;
        this.defaultTreatment = defaultTreatment;
        this.splitName = splitName;
        this.controlType = controlType;
        this.metrics = occupancyMetrics;
        this.segmentName = segmentName;
        this.type = type;
        this.channel = channel;
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
