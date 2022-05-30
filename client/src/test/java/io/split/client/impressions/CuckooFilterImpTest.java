package io.split.client.impressions;

import io.split.client.impressions.filters.CuckooFilterImp;
import org.junit.Assert;
import org.junit.Test;

public class CuckooFilterImpTest {

    @Test
    public void addSomeElements(){
        CuckooFilterImp cuckooFilter = new CuckooFilterImp(5,0.01);
        Assert.assertEquals(true, cuckooFilter.add("feature key-1"));
        Assert.assertEquals(true, cuckooFilter.add("feature key-2"));
        Assert.assertEquals(true, cuckooFilter.add("feature key-3"));
    }

    @Test
    public void checkContainSomeElements(){
        CuckooFilterImp cuckooFilter = new CuckooFilterImp(5,0.01);
        Assert.assertTrue(cuckooFilter.add("feature key-1"));
        Assert.assertTrue(cuckooFilter.add("feature key-2"));
        Assert.assertTrue(cuckooFilter.add("feature key-3"));

        Assert.assertEquals(true, cuckooFilter.contains("feature key-1"));
        Assert.assertEquals(true, cuckooFilter.contains("feature key-2"));
        Assert.assertEquals(true, cuckooFilter.contains("feature key-3"));
    }

    @Test
    public void removedElements(){
        CuckooFilterImp cuckooFilter = new CuckooFilterImp(5,0.01);
        cuckooFilter.add("feature key-1");
        cuckooFilter.add("feature key-2");
        cuckooFilter.add("feature key-3");

        cuckooFilter.clear();

        Assert.assertEquals(false, cuckooFilter.contains("feature key-1"));
        Assert.assertEquals(false, cuckooFilter.contains("feature key-2"));
        Assert.assertEquals(false, cuckooFilter.contains("feature key-3"));
    }
}