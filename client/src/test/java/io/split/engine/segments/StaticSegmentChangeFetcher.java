package io.split.engine.segments;

import com.google.common.collect.Lists;
import io.split.client.dtos.SegmentChange;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A wrapper around a set of keys. We return the entire set of keys, everytime
 * we are requested to fetch the latest set of changes.
 *
 * @author adil
 */
public class StaticSegmentChangeFetcher implements SegmentChangeFetcher {

    private final String _segmentName;
    private final List<String> _keys;

    public StaticSegmentChangeFetcher(String segmentName, Set<String> keys) {
        checkNotNull(keys);

        _segmentName = segmentName;
        _keys = Lists.newArrayList(keys);

        checkNotNull(_segmentName);
    }

    @Override
    public SegmentChange fetch(String segmentName, long changesSinceThisChangeNumber) {
        SegmentChange segmentChange = new SegmentChange();
        segmentChange.name = segmentName;
        segmentChange.since = changesSinceThisChangeNumber;
        segmentChange.till = 0;
        segmentChange.added = _keys;
        segmentChange.removed = Collections.<String>emptyList();

        return segmentChange;

    }
}
