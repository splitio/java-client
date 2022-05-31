package io.split.client.impressions.filters;

import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

public class BloomFilterImp implements Filter {

    BloomFilter bloomFilter;
    int spectedInsertions;
    double fpp;

    public BloomFilterImp(int spectedInsertions, double fpp) {
        this.spectedInsertions = spectedInsertions;
        this.fpp = fpp;
        this.bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_16), spectedInsertions, fpp);
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
        bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_16), spectedInsertions, fpp);

    }
}
