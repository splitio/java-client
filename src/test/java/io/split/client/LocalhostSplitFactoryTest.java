package io.split.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LocalhostSplitFactoryTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testNoFileYieldsEmptySet() throws IOException {
        LocalhostSplitFactory splitFactory = new LocalhostSplitFactory();

        // Check Split Client
        Assert.assertEquals(splitFactory.client().getClass(), LocalhostSplitClient.class);
        LocalhostSplitClient splitClient = (LocalhostSplitClient) splitFactory.client();
        Assert.assertThat(splitClient.featureToTreatmentMap().isEmpty(), Matchers.is(true));

        // Check Split Manager
        Assert.assertEquals(splitFactory.manager().getClass(), LocalhostSplitManager.class);
        LocalhostSplitManager splitManager = (LocalhostSplitManager) splitFactory.manager();
        Assert.assertThat(splitManager.featureToTreatmentMap().isEmpty(), Matchers.is(true));
    }

    @Test
    public void testParsingFile() throws IOException {
        File splitsFile = testFolder.newFile(LocalhostSplitFactory.FILENAME);
        List<String> fileContents = Lists.newArrayList("#foo", "", "   ", " typeahead_search on", "new_nav v3", "ux_changes");
        Files.write(splitsFile.toPath(), fileContents, StandardCharsets.UTF_8);

        LocalhostSplitFactory splitFactory = new LocalhostSplitFactory(splitsFile.getParent());

        Map<String, String> expected = Maps.newHashMap();
        expected.put("typeahead_search", "on");
        expected.put("new_nav", "v3");

        // Check Split Client
        Assert.assertEquals(splitFactory.client().getClass(), LocalhostSplitClient.class);
        LocalhostSplitClient splitClient = (LocalhostSplitClient) splitFactory.client();
        Assert.assertThat(splitClient.featureToTreatmentMap(), Matchers.is(Matchers.equalTo(expected)));

        // Check Split Manager
        Assert.assertEquals(splitFactory.manager().getClass(), LocalhostSplitManager.class);
        LocalhostSplitManager splitManager = (LocalhostSplitManager) splitFactory.manager();
        Assert.assertThat(splitManager.featureToTreatmentMap(), Matchers.is(Matchers.equalTo(expected)));
    }

    @Test
    public void testFileWatching() throws IOException, InterruptedException {
        // Create File
        File splitsFile = testFolder.newFile(LocalhostSplitFactory.FILENAME);
        Files.write(splitsFile.toPath(), Lists.newArrayList("test_1 on", "test_2 v3"), StandardCharsets.UTF_8);
        Thread.sleep(1000); // TODO: Find workaround for initial filesystem delay

        // Initialize Split Factory
        LocalhostSplitFactory splitFactory = new LocalhostSplitFactory(splitsFile.getParent());
        LocalhostSplitClient splitClient = (LocalhostSplitClient) splitFactory.client();
        LocalhostSplitManager splitManager = (LocalhostSplitManager) splitFactory.manager();

        // Update File
        Files.write(splitsFile.toPath(), Lists.newArrayList("test_1 on", "test_2 v4", "test_3 v1"), StandardCharsets.UTF_8);

        // Set Expected Value
        Map<String, String> expected = Maps.newHashMap();
        expected.put("test_1", "on");
        expected.put("test_2", "v4");
        expected.put("test_3", "v1");

        // Monitor for File Change
        // - MacOS implements a Polling watcher with a 2000ms delay
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 2500 &&
                !(Objects.equals(splitClient.featureToTreatmentMap(), expected)
                        && Objects.equals(splitManager.featureToTreatmentMap(), expected))
                ) {
            Thread.sleep(25);
        }

        // Confirm Update
        Assert.assertThat(splitClient.featureToTreatmentMap(), Matchers.is(Matchers.equalTo(expected)));
        Assert.assertThat(splitManager.featureToTreatmentMap(), Matchers.is(Matchers.equalTo(expected)));
    }

    private void updateSplitsFile(File splitsFile, String oldValue, String newValue) throws IOException {
        List<String> newLines = new ArrayList<>();
        for (String line : Files.readAllLines(splitsFile.toPath(), StandardCharsets.UTF_8)) {
            if (line.contains(oldValue)) {
                newLines.add(line.replace(oldValue, newValue));
            } else {
                newLines.add(line);
            }
        }
        Files.write(splitsFile.toPath(), newLines, StandardCharsets.UTF_8);
    }
}
