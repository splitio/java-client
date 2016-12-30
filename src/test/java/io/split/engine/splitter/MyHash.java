package io.split.engine.splitter;

import com.google.common.hash.Hashing;
import io.split.client.utils.MurmurHash3;

import java.nio.charset.Charset;

/**
 * Created by adilaijaz on 1/18/16.
 */
public interface MyHash {

    long hash(int seed, String key);

    public static class Murmur32Hash implements MyHash {
        @Override
        public long hash(int seed, String key) {
            return MurmurHash3.murmurhash3_x86_32(key, 0, key.length(), seed);
        }

        @Override
        public String toString() {
            return "murmur 32";
        }

    }

    public static class GuavaMurmur32Hash implements MyHash {

        private final Charset UTF_8 = Charset.forName("UTF-8");

        @Override
        public long hash(int seed, String key) {
            return Hashing.murmur3_32(seed).hashString(key, UTF_8).asInt();
        }

        @Override
        public String toString() {
            return "guava murmur 32";
        }

    }

    public static class SeededNaturalHash implements MyHash {

        @Override
        public long hash(int seed, String key) {
            int h = seed;
            for (int i = 0; i < key.length(); i++) {
                h = 31 * h + key.charAt(i);
            }
            return h;
        }

        public String toString() {
            return "seeded natural ";
        }
    }

    public static class XorNaturalHash implements MyHash {

        @Override
        public long hash(int seed, String key) {
            int h = 0;
            for (int i = 0; i < key.length(); i++) {
                h = 31 * h + key.charAt(i);
            }
            return h ^ seed;
        }

        public String toString() {
            return "xor seeded natural ";
        }
    }

    public static class LoseLoseHash implements MyHash {

        @Override
        public long hash(int seed, String key) {
            //char[] val = key.toCharArray();
            int h = seed;
            for (int i = 0; i < key.length(); i++) {
                h = h + key.charAt(i);
            }
            return h;
        }

        public String toString() {
            return "lose lose ";
        }
    }

}
