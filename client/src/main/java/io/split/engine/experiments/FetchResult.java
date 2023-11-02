package io.split.engine.experiments;

import java.util.Set;

public class FetchResult {
    private boolean _success;
    private boolean _retry;
    private Set<String> _segments;

    public FetchResult(boolean success, boolean retry, Set<String> segments) {
        _success = success;
        _retry = retry;
        _segments = segments;
    }

    public boolean isSuccess() {
        return _success;
    }
    public boolean retry() {
        return _retry;
    }

    public Set<String> getSegments() {
        return _segments;
    }
}
