package io.split.rules.bucketing;

import io.split.rules.model.Partition;

import java.util.List;

/**
 * Hashes keys into buckets and selects treatments from partition lists.
 */
public final class Bucketer {
    private static final int ALGO_LEGACY = 1;
    private static final int ALGO_MURMUR = 2;
    private static final String CONTROL = "control";

    /**
     * Returns the treatment for the given key, seed, partitions, and algorithm.
     * Returns "control" if no partition matches.
     */
    public static String getTreatment(String key, int seed, List<Partition> partitions, int algo) {
        if (partitions.isEmpty()) {
            return CONTROL;
        }
        if (hundredPercentOneTreatment(partitions)) {
            return partitions.get(0).treatment;
        }
        return selectTreatment(bucket(hash(key, seed, algo)), partitions);
    }

    /**
     * Returns a bucket between 1 and 100, inclusive.
     */
    public static int getBucket(String key, int seed, int algo) {
        return bucket(hash(key, seed, algo));
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

    static long murmurHash(String key, int seed) {
        return MurmurHash3.murmurhash3_x86_32(key, 0, key.length(), seed);
    }

    static int legacyHash(String key, int seed) {
        int h = 0;
        for (int i = 0; i < key.length(); i++) {
            h = 31 * h + key.charAt(i);
        }
        return h ^ seed;
    }

    static int bucket(long hash) {
        return (int) (Math.abs(hash % 100) + 1);
    }

    private static String selectTreatment(int bucket, List<Partition> partitions) {
        int covered = 0;
        for (Partition partition : partitions) {
            covered += partition.size;
            if (covered >= bucket) {
                return partition.treatment;
            }
        }
        return CONTROL;
    }

    private static boolean hundredPercentOneTreatment(List<Partition> partitions) {
        return partitions.size() == 1 && partitions.get(0).size == 100;
    }

    private Bucketer() {}
}
