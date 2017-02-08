package io.split.engine.segments;

import io.split.engine.segments.Segment;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A wrapper around a set of keys. There is no periodic refreshing happening here.
 *
 * @author adil
 */
public final class StaticSegment implements Segment {

    private final String _segmentName;
    private final Set<String> _keys;

    public StaticSegment(String segmentName, Set<String> keys) {
        _segmentName = segmentName;
        _keys = keys;

        checkNotNull(_segmentName);
        checkNotNull(_keys);
    }

    @Override
    public String segmentName() {
        return _segmentName;
    }

    @Override
    public boolean contains(String key) {
        return _keys.contains(key);
    }

    @Override
    public void forceRefresh() {
        return;
    }
}
