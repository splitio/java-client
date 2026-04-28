package io.split.client.impressions;

import io.split.client.impressions.filters.BloomFilterImp;
import io.split.client.impressions.filters.FilterAdapterImpl;
import org.junit.Assert;
import org.junit.Test;

public class FilterAdapterImplTest {

    @Test
    public void addSomeElements(){
        BloomFilterImp bloomFilterImp = new BloomFilterImp(5,0.01);
        FilterAdapterImpl filterAdapter = new FilterAdapterImpl(bloomFilterImp);

        Assert.assertEquals(true, filterAdapter.add("feature", "key-1"));
        Assert.assertEquals(true, filterAdapter.add("feature", "key-2"));
        Assert.assertEquals(true, filterAdapter.add("feature", "key-3"));
    }

    @Test
    public void checkContainSomeElements(){
        BloomFilterImp bloomFilterImp = new BloomFilterImp(5,0.01);
        FilterAdapterImpl filterAdapter = new FilterAdapterImpl(bloomFilterImp);

        Assert.assertTrue(filterAdapter.add("feature","key-1"));
        Assert.assertTrue(filterAdapter.add("feature","key-2"));
        Assert.assertTrue(filterAdapter.add("feature","key-3"));

        Assert.assertEquals(true, filterAdapter.contains("feature","key-1"));
        Assert.assertEquals(true, filterAdapter.contains("feature","key-2"));
        Assert.assertEquals(true, filterAdapter.contains("feature","key-3"));
    }

    @Test
    public void removedElements(){
        BloomFilterImp bloomFilterImp = new BloomFilterImp(5,0.01);
        FilterAdapterImpl filterAdapter = new FilterAdapterImpl(bloomFilterImp);
        filterAdapter.add("feature","key-1");
        filterAdapter.add("feature","key-2");
        filterAdapter.add("feature"," key-3");

        filterAdapter.clear();

        Assert.assertEquals(false, bloomFilterImp.contains("feature key-1"));
        Assert.assertEquals(false, bloomFilterImp.contains("feature key-2"));
        Assert.assertEquals(false, bloomFilterImp.contains("feature key-3"));
    }
}