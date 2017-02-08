package io.split.engine.segments;

import io.split.client.dtos.SegmentChange;

import java.util.Collections;

/**
 * First call returns a change, all subsequent calls return no change.
 *
 * @author adil
 */
public class NoChangeSegmentChangeFetcher implements SegmentChangeFetcher {

    @Override
    public SegmentChange fetch(String segmentName, long changesSinceThisChangeNumber) {
        SegmentChange segmentChange = new SegmentChange();
        segmentChange.name = segmentName;
        segmentChange.since = changesSinceThisChangeNumber;
        segmentChange.till = changesSinceThisChangeNumber;
        segmentChange.added = Collections.<String>emptyList();
        segmentChange.removed = Collections.<String>emptyList();

        return segmentChange;

    }

}
