package io.split.engine.splitter;

import io.split.client.dtos.Partition;
import io.split.client.utils.MurmurHash3;
import io.split.grammar.Treatments;

import java.util.List;

/**
 * These set of functions figure out which treatment a key should see.
 *
 * @author adil
 */
public class Splitter {
    private static final int ALGO_LEGACY = 1;
    private static final int ALGO_MURMUR = 2;

    public static String getTreatment(String key, int seed, List<Partition> partitions, int algo) {

        // 1. when there are no partitions, we just return control
        if (partitions.isEmpty()) {
            return Treatments.CONTROL;
        }


        if (hundredPercentOneTreatment(partitions)) {
            return partitions.get(0).treatment;
        }

        return getTreatment(bucket(hash(key, seed, algo)), partitions);
    }

    static long hash(String key, int seed, int algo) {
        switch (algo) {
            case ALGO_MURMUR:
                return murmurHash(key, seed);
            case ALGO_LEGACY:
            default:
                return legacyHash(key, seed);
        }
    }

    /*package private*/
    static long murmurHash(String key, int seed) {
        return MurmurHash3.murmurhash3_x86_32(key, 0, key.length(), seed);
    }

    /**
     * Returns a bucket between 1 and 100, inclusive.
     * @param key
     * @param seed
     * @return bucket >= 1 && bucket <= 100
     */
    public static int getBucket(String key, int seed, int algo) {
        return bucket(hash(key, seed, algo));
    }

    /*package private*/
    static int legacyHash(String key, int seed) {
        int h = 0;
        for (int i = 0; i < key.length(); i++) {
            h = 31 * h + key.charAt(i);
        }
        return h ^ seed;
    }

    /**
     * @param bucket
     * @param partitions MUST HAVE more than one partitions.
     * @return
     */
    private static String getTreatment(int bucket, List<Partition> partitions) {

        int bucketsCoveredThusFar = 0;

        for (Partition partition : partitions) {
            bucketsCoveredThusFar += partition.size;

            if (bucketsCoveredThusFar >= bucket) {
                return partition.treatment;
            }
        }

        return Treatments.CONTROL;
    }

    /*package private*/
    static int bucket(long hash) {
        return (int) (Math.abs(hash % 100) + 1);
    }


    private static boolean hundredPercentOneTreatment(List<Partition> partitions) {
        if (partitions.size() != 1) {
            return false;
        }

        return (partitions.get(0).size == 100);
    }

}
