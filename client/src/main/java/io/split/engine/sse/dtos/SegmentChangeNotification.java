package io.split.engine.sse.dtos;

public class SegmentChangeNotification extends IncomingNotification {
    private final long changeNumber;
    private final String segmentName;

    public SegmentChangeNotification(String channel, long changeNumber, String segmentName) {
        super(Type.SEGMENT_UPDATE, channel);
        this.changeNumber = changeNumber;
        this.segmentName = segmentName;
    }

    public String getSegmentName() {
        return segmentName;
    }

    public long getChangeNumber() {
        return changeNumber;
    }
}
