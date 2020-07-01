package io.split.engine.sse.dtos;

public class SegmentQueueDto {
    private final String segmentName;
    private final long changeNumber;

    public SegmentQueueDto(String segmentName, long changeNumber) {
        this.segmentName = segmentName;
        this.changeNumber = changeNumber;
    }

    public String getSegmentName() {
        return  segmentName;
    }

    public long getChangeNumber() {
        return  changeNumber;
    }

    @Override
    public String toString() {
        return String.format("segmentName: %s - changeNumber: %s", segmentName, changeNumber);
    }
}
