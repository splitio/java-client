package io.split.engine.sse.dtos;

public class SegmentChangeNotification extends IncomingNotification {
    private long changeNumber;
    private String segmentName;

    public String getSegmentName() {
        return segmentName;
    }

    public void setSegmentName(String segmentName) {
        this.segmentName = segmentName;
    }

    public long getChangeNumber() {
        return changeNumber;
    }

    public void setChangeNumber(long changeNumber) {
        this.changeNumber = changeNumber;
    }
}
