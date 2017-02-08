package io.split.engine.segments;

import com.google.common.collect.Lists;
import io.split.client.dtos.SegmentChange;

import java.util.Collections;

/**
 * First call returns a change, all subsequent calls return no change.
 *
 * @author adil
 */
public class OneChangeOnlySegmentChangeFetcher implements SegmentChangeFetcher {

    private volatile boolean _changeHappenedAlready = false;

    @Override
    public SegmentChange fetch(String segmentName, long changesSinceThisChangeNumber) {
        if (_changeHappenedAlready) {
            SegmentChange segmentChange = new SegmentChange();
            segmentChange.name = segmentName;
            segmentChange.since = changesSinceThisChangeNumber;
            segmentChange.till = changesSinceThisChangeNumber;
            segmentChange.added = Collections.<String>emptyList();
            segmentChange.removed = Collections.<String>emptyList();
            return segmentChange;
        }

        long latestChangeNumber = changesSinceThisChangeNumber + 1;

        SegmentChange segmentChange = new SegmentChange();
        segmentChange.name = segmentName;
        segmentChange.since = changesSinceThisChangeNumber;
        segmentChange.till = latestChangeNumber;
        segmentChange.added = Lists.newArrayList("" + latestChangeNumber);
        segmentChange.removed = Lists.newArrayList("" + changesSinceThisChangeNumber);

        _changeHappenedAlready = true;

        return segmentChange;

    }

    public boolean changeHappenedAlready() {
        return _changeHappenedAlready;
    }
}
