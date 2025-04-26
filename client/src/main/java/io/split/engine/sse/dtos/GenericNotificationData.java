package io.split.engine.sse.dtos;

import com.google.gson.annotations.SerializedName;

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
    private String featureFlagDefinition;
    @SerializedName("c")
    private Integer compressType;

    private GenericNotificationData (Long changeNumber,
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
        this.featureFlagDefinition = data;
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

    public String getDefinition() {
        return featureFlagDefinition;
    }

    public Integer getCompressType() {
        return compressType;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public static GenericNotificationData.Builder builder() {
        return new GenericNotificationData.Builder();
    }

    public static final class Builder {
        private Long changeNumber;
        private String defaultTreatment;
        private String featureFlagName;
        private ControlType controlType;
        private OccupancyMetrics metrics;
        private String segmentName;
        private IncomingNotification.Type type;
        private String channel;
        private Long previousChangeNumber;
        private String featureFlagDefinition;
        private Integer compressType;

        public Builder() {
        }

        public Builder changeNumber(Long changeNumber) {
            this.changeNumber = changeNumber;
            return this;
        }

        public Builder defaultTreatment(String defaultTreatment) {
            this.defaultTreatment = defaultTreatment;
            return this;
        }

        public Builder featureFlagName(String featureFlagName) {
            this.featureFlagName = featureFlagName;
            return this;
        }

        public Builder controlType(ControlType controlType) {
            this.controlType = controlType;
            return this;
        }

        public Builder metrics(OccupancyMetrics occupancyMetrics) {
            this.metrics = occupancyMetrics;
            return this;
        }

        public Builder segmentName(String segmentName) {
            this.segmentName = segmentName;
            return this;
        }

        public Builder type(IncomingNotification.Type type) {
            this.type = type;
            return this;
        }

        public Builder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public  Builder previousChangeNumber(Long previousChangeNumber) {
            this.previousChangeNumber = previousChangeNumber;
            return this;
        }

        public Builder featureFlagDefinition(String featureFlagDefinition) {
            this.featureFlagDefinition = featureFlagDefinition;
            return this;
        }

        public Builder compressType(Integer compressType) {
            this.compressType = compressType;
            return this;
        }

        public GenericNotificationData build() {
            return new GenericNotificationData(changeNumber, defaultTreatment, featureFlagName, controlType, metrics,
                    segmentName, type, channel, previousChangeNumber, featureFlagDefinition, compressType);
        }
    }
}