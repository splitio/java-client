package io.split.engine.segments;

import com.google.common.collect.Lists;
import io.split.client.dtos.SegmentChange;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A SegmentChangeFetcher useful for testing.
 */
public class AChangePerCallSegmentChangeFetcher implements SegmentChangeFetcher {

    private AtomicLong _lastAdded = new AtomicLong(-1L);

    @Override
    public SegmentChange fetch(String segmentName, long changesSinceThisChangeNumber) {
        long latestChangeNumber = changesSinceThisChangeNumber + 1;

        SegmentChange segmentChange = new SegmentChange();
        segmentChange.name = segmentName;
        segmentChange.since = changesSinceThisChangeNumber;
        segmentChange.till = latestChangeNumber;
        segmentChange.added = Lists.newArrayList("" + latestChangeNumber);
        segmentChange.removed = Lists.newArrayList("" + changesSinceThisChangeNumber);

        _lastAdded.set(latestChangeNumber);

        return segmentChange;
    }

    public long lastAdded() {
        return _lastAdded.get();
    }

}
