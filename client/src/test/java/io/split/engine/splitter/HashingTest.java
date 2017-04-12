package io.split.engine.splitter;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.util.*;

public class HashingTest {
    @Test
    public void numberOverflow() {
        int seed = (int) System.currentTimeMillis();

        List<String> keys = reallyLargeKeys(2000000, 10);

        MyHash hash = new MyHash.SeededNaturalHash();
        for (String key : keys) {
            hash.hash(seed, key);
        }
    }

    @Test
    public void spreadBySeedTest() {
        int seed1 = (int) System.currentTimeMillis();
        int seed2 = seed1 + 1;
        List<String> keys = randomUUIDs(216553);

        spreadBySeed(seed1, seed2, new MyHash.Murmur32Hash(), keys);
        spreadBySeed(seed1, seed2, new MyHash.XorNaturalHash(), keys);
    }

    @Test
    public void collisionTestForSequential() {
        int seed = (int) System.currentTimeMillis();

        List<String> sequentialKeys = sequentialIds(200000);
        collisionTest(seed, new MyHash.Murmur32Hash(), sequentialKeys);
        collisionTest(seed, new MyHash.GuavaMurmur32Hash(), sequentialKeys);
        collisionTest(seed, new MyHash.SeededNaturalHash(), sequentialKeys);
        collisionTest(seed, new MyHash.XorNaturalHash(), sequentialKeys);
    }

    @Test
    public void bucketTestForSequential() {
        int seed = (int) System.currentTimeMillis();

        List<String> sequentialKeys = mshIds();
        bucketTest(seed, new MyHash.Murmur32Hash(), sequentialKeys);
        bucketTest(seed, new MyHash.GuavaMurmur32Hash(), sequentialKeys);
        bucketTest(seed, new MyHash.SeededNaturalHash(), sequentialKeys);
        bucketTest(seed, new MyHash.XorNaturalHash(), sequentialKeys);
    }

    @Test
    public void bucketTestForRandomKeys() {

        int seed = (int) System.currentTimeMillis();

        List<String> randomKeys = randomUUIDs(100);
        bucketTest(seed, new MyHash.Murmur32Hash(), randomKeys);
        bucketTest(seed, new MyHash.GuavaMurmur32Hash(), randomKeys);
        bucketTest(seed, new MyHash.SeededNaturalHash(), randomKeys);
        bucketTest(seed, new MyHash.XorNaturalHash(), randomKeys);

    }

    @Test
    public void bucketTestForRandomKeys() {

        int seed = (int) System.currentTimeMillis();

        List<String> randomKeys = randomUUIDs(100);
        bucketTest(seed, new MyHash.Murmur32Hash(), randomKeys);
        bucketTest(seed, new MyHash.GuavaMurmur32Hash(), randomKeys);
        bucketTest(seed, new MyHash.SeededNaturalHash(), randomKeys);
        bucketTest(seed, new MyHash.XorNaturalHash(), randomKeys);

    }

    @Test
    public void collisionTestForRandom() {
        int seed = (int) System.currentTimeMillis();

        List<String> randomKeys = randomUUIDs(200000);
        collisionTest(seed, new MyHash.Murmur32Hash(), randomKeys);
        collisionTest(seed, new MyHash.GuavaMurmur32Hash(), randomKeys);
        collisionTest(seed, new MyHash.SeededNaturalHash(), randomKeys);
        collisionTest(seed, new MyHash.XorNaturalHash(), randomKeys);
    }

    @Test
    public void bucketTestForRandom() {
        int seed = (int) System.currentTimeMillis();

        List<String> randomKeys = randomUUIDs(200);
        bucketTest(seed, new MyHash.Murmur32Hash(), randomKeys);
        bucketTest(seed, new MyHash.GuavaMurmur32Hash(), randomKeys);
        bucketTest(seed, new MyHash.SeededNaturalHash(), randomKeys);
        bucketTest(seed, new MyHash.XorNaturalHash(), randomKeys);
    }


    private List<String> randomUUIDs(int size) {
        List<String> bldr = Lists.newArrayList();
        for (int i = 0; i < size; i++) {
            bldr.add(UUID.randomUUID().toString());
        }
        return bldr;
    }

    private List<String> sequentialIds(int size) {
        List<String> bldr = Lists.newArrayList();
        for (int i = 0; i < size; i++) {
            bldr.add("" + i);
        }
        return bldr;
    }

    private List<String> mshIds() {
        List<String> bldr = Lists.newArrayList();
        for (int i = 28243; i <= 28273; i++) {
            bldr.add("" + i);
        }
        return bldr;
    }

    private List<String> reallyLargeKeys(int keySize, int numKeys) {
        List<String> bldr = Lists.newArrayList();

        for (int i = 0; i < numKeys; i++) {
            bldr.add(RandomStringUtils.randomAlphanumeric(keySize));
        }

        return bldr;
    }

    private void collisionTest(int seed, MyHash hash, List<String> keys) {
        int collisions = 0;
        long durationSum = 0;

        Set<Long> hashes = Sets.newHashSet();

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            long start = System.nanoTime();
            long keyHash = hash.hash(seed, key);
            durationSum += (System.nanoTime() - start);

            if (!hashes.add(keyHash)) {
                collisions++;
            }
        }

        System.out.println(hash + " collisions: " + collisions + " percentage: " + (100f * collisions / keys.size()));
        System.out.println(hash + " time: " + durationSum / keys.size() + " ns");
    }

    private void bucketTest(int seed, MyHash hash, List<String> keys) {
        List<Integer> buckets = Lists.newArrayList();

        int[] ranges = new int[10];

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            long keyHash = hash.hash(seed, key);
            int bucket = Splitter.bucket(keyHash);
            buckets.add(bucket);
            ranges[(bucket - 1) / 10]++;
        }

        System.out.println(buckets);
        System.out.println(Arrays.toString(ranges));
    }

    private void spreadBySeed(int seed1, int seed2, MyHash hash, List<String> keys) {

        BitSet bitset = new BitSet();

        int i = 0;
        Iterator<String> iter = keys.iterator();
        while (iter.hasNext()) {
            String key = iter.next();

            if (hash.hash(seed1, key) == hash.hash(seed2, key)) {
                bitset.set(i);
            }

            i++;
        }

        int collisions = bitset.cardinality();
        System.out.println(hash + " collisions " + collisions + " percentage: " + (100f * collisions / keys.size()));
    }
}
