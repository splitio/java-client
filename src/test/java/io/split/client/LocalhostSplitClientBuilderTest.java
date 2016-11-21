package io.split.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by adilaijaz on 8/21/15.
 */
public class LocalhostSplitClientBuilderTest {

    @Before
    public void deleteSplitsFile() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        File f = new File(tmpDir, LocalhostSplitClientBuilder.FILENAME);
        f.delete();
    }

    @Test
    public void noFileMeansEmptySet() throws IOException {
        String tmpDir = System.getProperty("java.io.tmpdir");
        LocalhostSplitClient splitClient = LocalhostSplitClientBuilder.build(tmpDir);
        assertThat(splitClient.featureToTreatmentMap().isEmpty(), is(true));
    }

    @Test
    public void withFile() throws IOException {
        String tmpDir = System.getProperty("java.io.tmpdir");
        writeToSplitsFile(tmpDir, Lists.newArrayList("#foo", "", "   ", " typeahead_search on", "new_nav v3", "ux_changes"));

        LocalhostSplitClient splitClient = LocalhostSplitClientBuilder.build(tmpDir);


        Map<String, String> expected= Maps.newHashMap();
        expected.put("typeahead_search", "on");
        expected.put("new_nav", "v3");

        assertThat(splitClient.featureToTreatmentMap(), is(equalTo(expected)));
    }

    private void writeToSplitsFile(String dir, List<String> lines) throws IOException {
        File f = new File(dir, LocalhostSplitClientBuilder.FILENAME);
        f.deleteOnExit();
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));
        for (String line : lines) {
            writer.write(line + "\n");
        }
        writer.flush();
        writer.close();
    }


}
