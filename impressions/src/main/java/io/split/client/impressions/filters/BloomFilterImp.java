package io.split.client.impressions.filters;

import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

public class BloomFilterImp implements Filter {

    private BloomFilter<String> bloomFilter;
    private final int size;
    private final double errorMargin;

    public BloomFilterImp(int size, double errorMargin) {
        this.size = size;
        this.errorMargin = errorMargin;
        this.bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), size, errorMargin);
    }

    @Override
    public boolean add(String data) {
        return bloomFilter.put(data);
    }

    @Override
    public boolean contains(String data) {
        return bloomFilter.mightContain(data);
    }

    @Override
    public void clear() {
        bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_16), size, errorMargin);

    }
}
