package io.split.client.impressions.filters;

public class FilterAdapterImpl implements FilterAdapter {

    private final Filter filter;

    public FilterAdapterImpl(Filter filter) {
        this.filter = filter;
    }

    @Override
    public boolean add(String featureName, String key) {
        return filter.add(featureName + key);
    }

    @Override
    public boolean contains(String featureName, String key) {
        return filter.contains(featureName + key);
    }

    @Override
    public void clear() {
        filter.clear();
    }
}
