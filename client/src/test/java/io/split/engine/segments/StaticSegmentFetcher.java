package io.split.engine.segments;

import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.Map;

/**
 * Provides fetchers of type StaticSegmentFetcher.
 *
 * @author adil
 */
public class StaticSegmentFetcher implements SegmentFetcher, SegmentSynchronizationTask {

    private final ImmutableMap<String, StaticSegment> _staticSegmentFetchers;

    public StaticSegmentFetcher(Map<String, StaticSegment> staticSegmentFetchers) {
        _staticSegmentFetchers = ImmutableMap.copyOf(staticSegmentFetchers);
    }

    @Override
    public void fetch(){};

    public Segment segment(String segmentName) {
        StaticSegment segmentFetcher = _staticSegmentFetchers.get(segmentName);
        if (segmentFetcher == null) {
            segmentFetcher = new StaticSegment(segmentName, Collections.<String>emptySet());
        }
        return segmentFetcher;
    }

    @Override
    public void initializeSegment(String segmentName) {

    }

    @Override
    public SegmentFetcher getFetcher(String segmentName) {
        return null;
    }

    @Override
    public void startPeriodicFetching() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void run() {

    }
}
