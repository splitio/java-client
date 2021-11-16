package io.split.client.impressions;

import io.split.client.dtos.KeyImpression;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class InMemoryImpressionsStorageTest {

    @Test
    public void testBasicUsage() {
        InMemoryImpressionsStorage storage = new InMemoryImpressionsStorage(10);
        for (int i = 0; i < 15; i++) {
            if (i < 10) {
                assertThat(storage.put(Stream.of(new KeyImpression()).collect(Collectors.toList())), is(1L));
            } else {
                assertThat(storage.put(Stream.of(new KeyImpression()).collect(Collectors.toList())), is(0L));
            }
        }

        assertThat(storage.isFull(), is(true));
        List<KeyImpression> res = storage.pop(15);
        assertThat(res.size(), is(equalTo(10)));
        assertThat(storage.isFull(), is(false));
    }
}
