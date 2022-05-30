package io.split.client.impressions.filters;

import io.split.client.impressions.filters.Filter;
import io.split.client.impressions.filters.FilterAdapter;

public class FilterAdapterImpl implements FilterAdapter {

    Filter filter;

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

    }
}
