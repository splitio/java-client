package io.split.engine.matchers;

import io.split.engine.segments.Segment;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A matcher that checks if the key is part of a user defined segment. This class
 * assumes that the logic for refreshing what keys are part of a segment is delegated
 * to SegmentFetcher.
 *
 * @author adil
 */
public class UserDefinedSegmentMatcher implements Matcher {
    private final String _segmentName;
    private final Segment _segment;

    public UserDefinedSegmentMatcher(Segment segment) {
        checkNotNull(segment);
        _segmentName = segment.segmentName();
        _segment = segment;
        checkNotNull(_segmentName);
    }


    @Override
    public boolean match(Object key) {
        if (!(key instanceof String)) {
            return false;
        }
        return _segment.contains((String) key);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + _segmentName.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof UserDefinedSegmentMatcher)) return false;

        UserDefinedSegmentMatcher other = (UserDefinedSegmentMatcher) obj;

        return _segmentName.equals(other._segmentName);
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("in segment ");
        bldr.append(_segmentName);
        return bldr.toString();
    }
}
