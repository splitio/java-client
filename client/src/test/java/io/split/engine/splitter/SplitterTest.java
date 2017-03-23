package io.split.engine.splitter;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import io.split.client.dtos.Partition;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Test for Splitter.
 *
 * @author adil
 */
public class SplitterTest {

    @Ignore
    @Test
    public void generateData() {
        Random r = new Random();
        int minKeyLength = 7;

        for (int j = 0; j < 100; j++) {
            int seed = r.nextInt();
            for (int i = 0; i < 1000; i++) {
                int keyLength = minKeyLength + r.nextInt(13);
                String key = RandomStringUtils.randomAlphanumeric(keyLength);
                int hash = Splitter.hash(key, seed);
                int bucket = Splitter.bucket(hash);
                System.out.println(Joiner.on(',').join(Lists.newArrayList(seed, key, hash, bucket)));
            }
        }

    }

    @Ignore
    @Test
    public void generateNonAlphaNumericData() {
        Random r = new Random();
        int minKeyLength = 7;

        for (int j = 0; j < 100; j++) {
            int seed = r.nextInt();
            for (int i = 0; i < 1000; i++) {
                int keyLength = minKeyLength + r.nextInt(13);
                String key = RandomStringUtils.random(keyLength);
                int hash = Splitter.hash(key, seed);
                int bucket = Splitter.bucket(hash);
                System.out.println(Joiner.on(',').join(Lists.newArrayList(seed, key, hash, bucket)));
            }
        }

    }

    @Test
    public void works() {
        List<Partition> partitions = Lists.newArrayList();
        for (int i = 1; i <= 100; i++) {
            partitions.add(partition("" + i, 1));
        }

        int[] treatments = new int[100];

        int n = 100000;
        double p = 0.01d;

        for (int i = 0; i < n; i++) {
            String key = RandomStringUtils.random(20);
            String treatment = Splitter.getTreatment(key, 123, partitions);
            treatments[Integer.parseInt(treatment) - 1]++;
        }

        double mean = n * p;
        double stddev = Math.sqrt(mean * (1 - p));

        int min = (int) (mean - 4 * stddev);
        int max = (int) (mean + 4 * stddev);

        for (int i = 0; i < treatments.length; i++) {
            assertThat(String.format("Value: " + treatments[i] + " is out of range [%s, %s]", min, max), treatments[i] >= min && treatments[i] <= max, is(true));
        }
    }

    @Test
    public void ifHundredPercentOneTreatmentWeShortcut() {
        Partition partition = partition("on", 100);

        List<Partition> partitions = Lists.newArrayList(partition);

        assertThat(Splitter.getTreatment("13", 15, partitions), is(equalTo("on")));
    }

    @Test
    public void getBucket() {
        System.out.println(Splitter.getBucket("pato@split.io", 123));
    }

    private Partition partition(String treatment, int size) {
        Partition p = new Partition();
        p.treatment = treatment;
        p.size = size;
        return p;
    }
}
