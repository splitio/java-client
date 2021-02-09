package io.split.client;

import com.google.common.collect.Maps;
import io.split.cache.InMemoryCacheImp;
import io.split.cache.SplitCache;
import io.split.grammar.Treatments;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests for LocalhostSplitClient
 *
 * @author adil
 */
public class LocalhostSplitClientTest {

    @Test
    public void defaultsWork() throws URISyntaxException {
        Map<SplitAndKey, LocalhostSplit> map = Maps.newHashMap();
        map.put(SplitAndKey.of("onboarding"), LocalhostSplit.of("on"));
        map.put(SplitAndKey.of("test"), LocalhostSplit.of("a"));
        map.put(SplitAndKey.of("onboarding"), LocalhostSplit.of("off")); // overwrite

        SplitCache splitCache = new InMemoryCacheImp();
        LocalhostSplitClient client = new LocalhostSplitClient(map, splitCache);

        assertThat(client.getTreatment(null, "foo"), is(equalTo(Treatments.CONTROL)));
        assertThat(client.getTreatment("user1", "foo"), is(equalTo(Treatments.CONTROL)));
        assertThat(client.getTreatment("user1", "onboarding"), is(equalTo("off")));
        assertThat(client.getTreatment("user2", "onboarding"), is(equalTo("off")));
        assertThat(client.getTreatment("user1", "test"), is(equalTo("a")));
        assertThat(client.getTreatment("user2", "test"), is(equalTo("a")));
        assertThat(client.getTreatmentWithConfig("user2", "test").config(), is(nullValue()));
        assertThat(client.getTreatmentWithConfig("user2", "test").treatment(), is(equalTo("a")));
    }

    @Test
    public void overrides_work() throws URISyntaxException {
        Map<SplitAndKey, LocalhostSplit> map = Maps.newHashMap();
        map.put(SplitAndKey.of("onboarding"), LocalhostSplit.of("on"));
        map.put(SplitAndKey.of("onboarding", "user1"), LocalhostSplit.of("off"));
        map.put(SplitAndKey.of("onboarding", "user2"), LocalhostSplit.of("off"));

        SplitCache splitCache = new InMemoryCacheImp();
        LocalhostSplitClient client = new LocalhostSplitClient(map, splitCache);

        assertThat(client.getTreatment("user1", "onboarding"), is(equalTo("off")));
        assertThat(client.getTreatment("user2", "onboarding"), is(equalTo("off")));
        assertThat(client.getTreatment("user3", "onboarding"), is(equalTo("on")));
        assertThat(client.getTreatmentWithConfig("user3", "onboarding").config(), is(nullValue()));
        assertThat(client.getTreatmentWithConfig("user3", "onboarding").treatment(), is(equalTo("on")));
    }

    @Test
    public void if_only_overrides_exist() throws URISyntaxException {
        Map<SplitAndKey, LocalhostSplit> map = Maps.newHashMap();
        map.put(SplitAndKey.of("onboarding", "user1"), LocalhostSplit.of("off"));
        map.put(SplitAndKey.of("onboarding", "user2"), LocalhostSplit.of("off"));

        SplitCache splitCache = new InMemoryCacheImp();
        LocalhostSplitClient client = new LocalhostSplitClient(map, splitCache);

        assertThat(client.getTreatment("user1", "onboarding"), is(equalTo("off")));
        assertThat(client.getTreatment("user2", "onboarding"), is(equalTo("off")));
        assertThat(client.getTreatment("user3", "onboarding"), is(equalTo(Treatments.CONTROL)));
    }

    @Test
    public void attributes_work() throws URISyntaxException {
        Map<SplitAndKey, LocalhostSplit> map = Maps.newHashMap();
        map.put(SplitAndKey.of("onboarding"), LocalhostSplit.of("on"));
        map.put(SplitAndKey.of("onboarding", "user1"), LocalhostSplit.of("off"));
        map.put(SplitAndKey.of("onboarding", "user2"), LocalhostSplit.of("off"));

        SplitCache splitCache = new InMemoryCacheImp();
        LocalhostSplitClient client = new LocalhostSplitClient(map, splitCache);

        Map<String, Object> attributes = Maps.newHashMap();
        attributes.put("age", 24);

        assertThat(client.getTreatment("user1", "onboarding", attributes), is(equalTo("off")));
        assertThat(client.getTreatment("user2", "onboarding", attributes), is(equalTo("off")));
        assertThat(client.getTreatment("user3", "onboarding", attributes), is(equalTo("on")));
    }

    @Test
    public void update_works() throws URISyntaxException {
        Map<SplitAndKey, LocalhostSplit> map = Maps.newHashMap();
        map.put(SplitAndKey.of("onboarding"), LocalhostSplit.of("on"));
        map.put(SplitAndKey.of("onboarding", "user1"), LocalhostSplit.of("off"));
        map.put(SplitAndKey.of("onboarding", "user2"), LocalhostSplit.of("off"));

        SplitCache splitCache = new InMemoryCacheImp();
        LocalhostSplitClient client = new LocalhostSplitClient(map, splitCache);

        assertThat(client.getTreatment("user1", "onboarding"), is(equalTo("off")));
        assertThat(client.getTreatment("user2", "onboarding"), is(equalTo("off")));
        assertThat(client.getTreatment("user3", "onboarding"), is(equalTo("on")));

        map.clear();
        map.put(SplitAndKey.of("onboarding"), LocalhostSplit.of("on"));
        map.put(SplitAndKey.of("onboarding", "user1"), LocalhostSplit.of("off"));

        client.updateFeatureToTreatmentMap(map);

        assertThat(client.getTreatment("user1", "onboarding"), is(equalTo("off")));
        assertThat(client.getTreatment("user2", "onboarding"), is(equalTo("on")));
        assertThat(client.getTreatment("user3", "onboarding"), is(equalTo("on")));
    }
}
