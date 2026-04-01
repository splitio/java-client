package io.split.rules.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Prerequisite {
    private final String _featureFlagName;
    private final List<String> _treatments;

    public Prerequisite(String featureFlagName, List<String> treatments) {
        _featureFlagName = Objects.requireNonNull(featureFlagName);
        _treatments = Collections.unmodifiableList(treatments);
    }

    public String featureFlagName() {
        return _featureFlagName;
    }

    public List<String> treatments() {
        return _treatments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Prerequisite that = (Prerequisite) o;
        return Objects.equals(_featureFlagName, that._featureFlagName)
                && Objects.equals(_treatments, that._treatments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_featureFlagName, _treatments);
    }
}
