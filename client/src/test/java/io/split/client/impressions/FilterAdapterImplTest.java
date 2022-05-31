package io.split.client.impressions;

import io.split.client.impressions.filters.BloomFilterImp;
import io.split.client.impressions.filters.FilterAdapterImpl;
import org.junit.Assert;
import org.junit.Test;

public class FilterAdapterImplTest {

    @Test
    public void addSomeElements(){
        BloomFilterImp cuckooFilter = new BloomFilterImp(5,0.01);
        FilterAdapterImpl filterAdapter = new FilterAdapterImpl(cuckooFilter);

        Assert.assertEquals(true, filterAdapter.add("feature", "key-1"));
        Assert.assertEquals(true, filterAdapter.add("feature", "key-2"));
        Assert.assertEquals(true, filterAdapter.add("feature", "key-3"));
    }

    @Test
    public void checkContainSomeElements(){
        BloomFilterImp cuckooFilter = new BloomFilterImp(5,0.01);
        FilterAdapterImpl filterAdapter = new FilterAdapterImpl(cuckooFilter);

        Assert.assertTrue(filterAdapter.add("feature","key-1"));
        Assert.assertTrue(filterAdapter.add("feature","key-2"));
        Assert.assertTrue(filterAdapter.add("feature","key-3"));

        Assert.assertEquals(true, filterAdapter.contains("feature","key-1"));
        Assert.assertEquals(true, filterAdapter.contains("feature","key-2"));
        Assert.assertEquals(true, filterAdapter.contains("feature","key-3"));
    }

    @Test
    public void removedElements(){
        BloomFilterImp cuckooFilter = new BloomFilterImp(5,0.01);
        FilterAdapterImpl filterAdapter = new FilterAdapterImpl(cuckooFilter);
        filterAdapter.add("feature","key-1");
        filterAdapter.add("feature","key-2");
        filterAdapter.add("feature"," key-3");

        filterAdapter.clear();

        Assert.assertEquals(false, cuckooFilter.contains("feature key-1"));
        Assert.assertEquals(false, cuckooFilter.contains("feature key-2"));
        Assert.assertEquals(false, cuckooFilter.contains("feature key-3"));
    }
}