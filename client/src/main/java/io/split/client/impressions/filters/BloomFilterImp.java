package io.split.client.impressions.filters;

import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

public class BloomFilterImp implements Filter {

    private BloomFilter bloomFilter;
    private final int size;
    private final double errorMargin;

    public BloomFilterImp(int size, double errorMargin) {
        this.size = size;
        this.errorMargin = errorMargin;
        this.bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), size, errorMargin);
    }

    @Override
    public synchronized boolean add(String data) {
        return bloomFilter.put(data);
    }

    @Override
    public synchronized boolean contains(String data) {
        return bloomFilter.mightContain(data);
    }

    @Override
    public synchronized void clear() {
        bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_16), size, errorMargin);

    }
}
