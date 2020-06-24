package io.split.engine.sse.dtos;

public class SplitKillNotification extends IncomingNotification {
    private final long changeNumber;
    private final String defaultTreatment;
    private final String splitName;

    public  SplitKillNotification(String channel, long changeNumber, String defaultTreatment, String splitName) {
        super(Type.SPLIT_KILL, channel);
        this.changeNumber = changeNumber;
        this.defaultTreatment = defaultTreatment;
        this.splitName = splitName;
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
}
