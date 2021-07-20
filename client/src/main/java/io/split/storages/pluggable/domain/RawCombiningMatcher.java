package io.split.storages.pluggable.domain;

import io.split.client.dtos.MatcherCombiner;
import io.split.engine.matchers.AttributeMatcher;

import java.util.List;

public class RawCombiningMatcher {
    private List<AttributeMatcher> _delegates;
    private MatcherCombiner _combiner;
}
