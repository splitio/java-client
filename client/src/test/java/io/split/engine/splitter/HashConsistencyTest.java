package io.split.engine.splitter;

import com.google.common.hash.Hashing;
import io.split.client.utils.MurmurHash3;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

public class HashConsistencyTest {
    @Test
    public void testLegacyHashAlphaNum() throws IOException {
        URL resource = getClass().getClassLoader().getResource("legacy-hash-sample-data.csv");
        File file = new File(resource.getFile());
        validateFileLegacyHash(file);
    }

    @Test
    @Ignore
    public void testLegacyHashNonAlphaNum() throws IOException {
        URL resource = getClass().getClassLoader().getResource("legacy-hash-sample-data-non-alpha-numeric.csv");
        File file = new File(resource.getFile());
        validateFileLegacyHash(file);
    }

    @Test
    public void testMurmur3HashAlphaNum() throws IOException {
        URL resource = getClass().getClassLoader().getResource("murmur3-sample-data-v2.csv");
        File file = new File(resource.getFile());
        validateFileMurmur3Hash(file);
    }

    @Test
    public void testMurmur3HashNonAlphaNum() throws IOException {
        URL resource = getClass().getClassLoader().getResource("murmur3-sample-data-non-alpha-numeric-v2.csv");
        File file = new File(resource.getFile());
        validateFileMurmur3Hash(file);
    }

    @Test
    public void testGuavaMurmur3HashAlphaNum() throws IOException {
        URL resource = getClass().getClassLoader().getResource("murmur3-sample-data.csv");
        File file = new File(resource.getFile());
        validateFileGuavaMurmur3Hash(file);
    }

    @Test
    public void testGuavaMurmur3HashNonAlphaNum() throws IOException {
        URL resource = getClass().getClassLoader().getResource("murmur3-sample-data-non-alpha-numeric.csv");
        File file = new File(resource.getFile());
        validateFileGuavaMurmur3Hash(file);
    }

    @Test
    public void testMurmurHash3_64() throws IOException {
        URL resource = getClass().getClassLoader().getResource("murmur3_64_uuids.csv");
        File file = new File(resource.getFile());
        validateFileMurmurHash3_64(file);
    }

    private void validateFileLegacyHash(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        reader.readLine(); // Header

        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            Integer seed = Integer.parseInt(parts[0]);
            String key = parts[1];
            int expected_hash = Integer.parseInt(parts[2]);
            int expected_bucket = Integer.parseInt(parts[3]);

            int hash = Splitter.legacy_hash(key, seed);
            int bucket = Splitter.bucket(hash);

            Assert.assertEquals(expected_hash, hash);
            Assert.assertEquals(expected_bucket, bucket);
        }
    }

    private void validateFileMurmur3Hash(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        reader.readLine(); // Header

        String line;
        int i = 0;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            Integer seed = Integer.parseInt(parts[0]);
            String key = parts[1];
            long expected_hash = Long.parseLong(parts[2]);
            int expected_bucket = Integer.parseInt(parts[3]);

            long hash = Splitter.murmur_hash(key, seed);
            int bucket = Splitter.bucket(hash);

            Assert.assertEquals(expected_hash, hash);
            Assert.assertEquals(expected_bucket, bucket);
        }
    }

    private void validateFileGuavaMurmur3Hash(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        reader.readLine(); // Header

        String line;
        int i = 0;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            Integer seed = Integer.parseInt(parts[0]);
            String key = parts[1];
            long expected_hash = Long.parseLong(parts[2]);
            int expected_bucket = Integer.parseInt(parts[3]);

            int hash = Hashing.murmur3_32(seed).hashString(key, Charset.forName("UTF-8")).asInt();

            int bucket = Splitter.bucket(hash);

            Assert.assertEquals(expected_hash, hash);
            Assert.assertEquals(expected_bucket, bucket);
        }
    }

    private void validateFileMurmurHash3_64(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        reader.readLine(); // Header

        String line;
        int i = 0;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            String key = parts[0];
            long seed = Long.parseLong(parts[1]);
            long expected_hash = Long.parseLong(parts[2]);
            long[] hash = MurmurHash3.hash128x64(key.getBytes(), 0, key.length(), seed);
            Assert.assertEquals(expected_hash, hash[0]);
        }
    }
}
