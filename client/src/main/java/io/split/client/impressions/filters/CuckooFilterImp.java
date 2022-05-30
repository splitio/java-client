package io.split.client.impressions.filters;

import com.duprasville.guava.probably.CuckooFilter;
import com.google.common.base.Charsets;
import com.google.common.hash.Funnels;
import io.split.client.impressions.filters.Filter;

public class CuckooFilterImp implements Filter {

    CuckooFilter cuckooFilter;

    public CuckooFilterImp(int spectedInsertions, double fpp) {
        this.cuckooFilter = CuckooFilter.create(Funnels.stringFunnel(Charsets.UTF_16), spectedInsertions, fpp);
    }

    @Override
    public boolean add(String data) {
        return cuckooFilter.add(data);
    }

    @Override
    public boolean contains(String data) {
        return cuckooFilter.contains(data);
    }

    @Override
    public void clear() {
        cuckooFilter.clear();

    }
}
