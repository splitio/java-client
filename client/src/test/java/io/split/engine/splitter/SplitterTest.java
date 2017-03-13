package io.split.engine.splitter;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import io.split.client.dtos.Partition;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
                long hash = Splitter.hash(key, seed);
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
                long hash = Splitter.hash(key, seed);
                int bucket = Splitter.bucket(hash);
                System.out.println(Joiner.on(',').join(Lists.newArrayList(seed, key, hash, bucket)));
            }
        }

    }

    /**
     * Use this utily method when algos changes are required and you need to
     * generate another sample file using existing seed and key input from
     * another file
     *
     * @throws IOException
     */
    @Ignore
    @Test
    public void generateDataFromExistingInput() throws IOException {
        File file = new File("src/test/resources", "murmur3-sample-data-non-alpha-numeric.csv");
        BufferedReader reader = new BufferedReader(new FileReader(file));
        reader.readLine(); // Header

        File target = new File("src/test/resources", "murmur3-sample-data-non-alpha-numeric-v2.csv");
        BufferedWriter writer = new BufferedWriter(new FileWriter(target));

        // Writer header.
        writer.append("# seed, key, hash, bucket");
        writer.newLine();

        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            Integer seed = Integer.parseInt(parts[0]);
            String key = parts[1];
            long hash = Splitter.hash(key, seed);
            int bucket = Splitter.bucket(hash);
            writer.append(Joiner.on(',').join(Lists.newArrayList(seed, key, hash, bucket)));
            writer.newLine();
        }
        writer.close();
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
            String treatment = Splitter.getTreatment(key, 123, partitions, 1);
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

        assertThat(Splitter.getTreatment("13", 15, partitions, 1), is(equalTo("on")));
    }

    private Partition partition(String treatment, int size) {
        Partition p = new Partition();
        p.treatment = treatment;
        p.size = size;
        return p;
    }
}
