package io.split.client.impressions;

import io.split.client.impressions.filters.BloomFilterImp;
import org.junit.Assert;
import org.junit.Test;

public class BloomFilterImpTest {

    @Test
    public void addSomeElements(){
        BloomFilterImp bloomFilterImp = new BloomFilterImp(5,0.01);
        Assert.assertEquals(true, bloomFilterImp.add("feature key-1"));
        Assert.assertEquals(true, bloomFilterImp.add("feature key-2"));
        Assert.assertEquals(true, bloomFilterImp.add("feature key-3"));
    }

    @Test
    public void checkContainSomeElements(){
        BloomFilterImp bloomFilterImp = new BloomFilterImp(5,0.01);
        Assert.assertTrue(bloomFilterImp.add("feature key-1"));
        Assert.assertTrue(bloomFilterImp.add("feature key-2"));
        Assert.assertTrue(bloomFilterImp.add("feature key-3"));

        Assert.assertEquals(true, bloomFilterImp.contains("feature key-1"));
        Assert.assertEquals(true, bloomFilterImp.contains("feature key-2"));
        Assert.assertEquals(true, bloomFilterImp.contains("feature key-3"));
    }

    @Test
    public void removedElements(){
        BloomFilterImp bloomFilterImp = new BloomFilterImp(5,0.01);
        bloomFilterImp.add("feature key-1");
        bloomFilterImp.add("feature key-2");
        bloomFilterImp.add("feature key-3");

        bloomFilterImp.clear();

        Assert.assertEquals(false, bloomFilterImp.contains("feature key-1"));
        Assert.assertEquals(false, bloomFilterImp.contains("feature key-2"));
        Assert.assertEquals(false, bloomFilterImp.contains("feature key-3"));
    }
}