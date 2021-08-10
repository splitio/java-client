package io.split.engine.experiments;

import java.util.Set;

public class FetchResult {
    private boolean _success;
    private Set<String> _segments;

    public FetchResult(boolean success, Set<String> segments) {
        _success = success;
        _segments = segments;
    }

    public boolean isSuccess() {
        return _success;
    }

    public Set<String> getSegments() {
        return _segments;
    }
}
