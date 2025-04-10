package io.split.client;

import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import io.split.client.utils.FileInputStreamProvider;
import io.split.client.utils.InputStreamProvider;
import io.split.client.utils.LocalhostUtils;
import io.split.engine.common.FetchOptions;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YamlLocalhostSplitChangeFetcherTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testParseSplitChange() throws IOException {
        File file = folder.newFile(SplitClientConfig.LOCALHOST_DEFAULT_FILE);

        List<Map<String, Object>> allSplits = new ArrayList();

        Map<String, Object> split1_user_a = new LinkedHashMap<>();
        Map<String, Object> split1_user_a_data = new LinkedHashMap<>();
        split1_user_a_data.put("keys", "user_a");
        split1_user_a_data.put("treatment", "off");
        split1_user_a_data.put("config", "{ \"size\" : 20 }");
        split1_user_a.put("split_1", split1_user_a_data);
        allSplits.add(split1_user_a);

        Map<String, Object> split1_user_b = new LinkedHashMap<>();
        Map<String, Object> split1_user_b_data = new LinkedHashMap<>();
        split1_user_b_data.put("keys", "user_b");
        split1_user_b_data.put("treatment", "on");
        split1_user_b.put("split_1", split1_user_b_data);
        allSplits.add(split1_user_b);

        Map<String, Object> split2_user_a = new LinkedHashMap<>();
        Map<String, Object> split2_user_a_data = new LinkedHashMap<>();
        split2_user_a_data.put("keys", "user_a");
        split2_user_a_data.put("treatment", "off");
        split2_user_a_data.put("config", "{ \"size\" : 20 }");
        split2_user_a.put("split_2", split2_user_a_data);
        allSplits.add(split2_user_a);


        Yaml yaml = new Yaml();
        StringWriter writer = new StringWriter();
        yaml.dump(allSplits, writer);
        LocalhostUtils.writeFile(file, writer);

        InputStreamProvider inputStreamProvider = new FileInputStreamProvider(file.getAbsolutePath());
        YamlLocalhostSplitChangeFetcher localhostSplitChangeFetcher = new YamlLocalhostSplitChangeFetcher(inputStreamProvider);
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);
        SplitChange splitChange = localhostSplitChangeFetcher.fetch(-1L, -1, fetchOptions);

        Assert.assertEquals(2, splitChange.splits.size());
        Assert.assertEquals(-1, splitChange.since);
        Assert.assertEquals(-1, splitChange.till);


        for (Split split: splitChange.splits) {
            Assert.assertEquals("control", split.defaultTreatment);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void processTestForException() {
        InputStreamProvider inputStreamProvider = new FileInputStreamProvider("src/test/resources/notExist.yaml");
        YamlLocalhostSplitChangeFetcher localhostSplitChangeFetcher = new YamlLocalhostSplitChangeFetcher(inputStreamProvider);
        FetchOptions fetchOptions = Mockito.mock(FetchOptions.class);

        SplitChange splitChange = localhostSplitChangeFetcher.fetch(-1L, -1, fetchOptions);
    }
}