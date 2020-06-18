package io.split.engine.sse.dtos;

public class SplitKillNotification extends IncomingNotification {
    private long changeNumber;
    private String defaultTreatment;
    private String splitName;

    public long getChangeNumber() {
        return changeNumber;
    }

    public void setChangeNumber(long changeNumber) {
        this.changeNumber = changeNumber;
    }

    public String getDefaultTreatment() {
        return defaultTreatment;
    }

    public void setDefaultTreatment(String defaultTreatment) {
        this.defaultTreatment = defaultTreatment;
    }

    public String getSplitName() {
        return splitName;
    }

    public void setSplitName(String splitName) {
        this.splitName = splitName;
    }
}
