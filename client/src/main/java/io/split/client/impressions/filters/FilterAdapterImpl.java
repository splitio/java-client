package io.split.client.impressions.filters;

public class FilterAdapterImpl implements FilterAdapter {

    private final Filter filter;

    public FilterAdapterImpl(Filter filter) {
        this.filter = filter;
    }

    @Override
    public boolean add(String featureFlagName, String key) {
        return filter.add(featureFlagName + key);
    }

    @Override
    public boolean contains(String featureFlagName, String key) {
        return filter.contains(featureFlagName + key);
    }

    @Override
    public void clear() {
        filter.clear();
    }
}
