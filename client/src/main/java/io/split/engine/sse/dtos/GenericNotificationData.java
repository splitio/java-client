package io.split.engine.sse.dtos;

import io.split.engine.sse.enums.CompressType;

public class GenericNotificationData {
    private final Long changeNumber;
    private final String defaultTreatment;
    private final String splitName;
    private final ControlType controlType;
    private final OccupancyMetrics metrics;
    private final String segmentName;
    private final IncomingNotification.Type type;
    private String channel;
    private final Long pcn;
    private final String d;
    private final CompressType c;

    public GenericNotificationData (Long changeNumber,
                                    String defaultTreatment,
                                    String splitName,
                                    ControlType controlType,
                                    OccupancyMetrics occupancyMetrics,
                                    String segmentName,
                                    IncomingNotification.Type type,
                                    String channel,
                                    Long pcn,
                                    String d,
                                    CompressType c) {
        this.changeNumber = changeNumber;
        this.defaultTreatment = defaultTreatment;
        this.splitName = splitName;
        this.controlType = controlType;
        this.metrics = occupancyMetrics;
        this.segmentName = segmentName;
        this.type = type;
        this.channel = channel;
        this.pcn = pcn;
        this.d = d;
        this.c = c;
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
    public Long getPcn() {
        return pcn;
    }

    public String getD() {
        return d;
    }

    public CompressType getC() {
        return c;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}