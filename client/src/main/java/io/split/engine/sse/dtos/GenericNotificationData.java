package io.split.engine.sse.dtos;

import com.google.gson.annotations.SerializedName;
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
    @SerializedName("pcn")
    private Long previousChangeNumber;
    @SerializedName("d")
    private String data;
    @SerializedName("c")
    private Integer compressType;

    public GenericNotificationData (Long changeNumber,
                                    String defaultTreatment,
                                    String splitName,
                                    ControlType controlType,
                                    OccupancyMetrics occupancyMetrics,
                                    String segmentName,
                                    IncomingNotification.Type type,
                                    String channel,
                                    Long previousChangeNumber,
                                    String data,
                                    Integer compressType) {
        this.changeNumber = changeNumber;
        this.defaultTreatment = defaultTreatment;
        this.splitName = splitName;
        this.controlType = controlType;
        this.metrics = occupancyMetrics;
        this.segmentName = segmentName;
        this.type = type;
        this.channel = channel;
        this.previousChangeNumber = previousChangeNumber;
        this.data = data;
        this.compressType = compressType;
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
    public Long getPreviousChangeNumber() {
        return previousChangeNumber;
    }

    public String getData() {
        return data;
    }

    public Integer getCompressType() {
        return compressType;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}