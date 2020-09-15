package io.split.client.dtos;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

public class ImpressionCountTest {

    @Test
    public void testImpressionCountSerialization() {
        ImpressionCount ic = new ImpressionCount(Collections.singletonList(
                new ImpressionCount.CountPerFeature("test1", 0, 23)));

        Gson gson = new Gson();
        String serialized = gson.toJson(ic);
        HashMap<String, Object> parsedRaw = gson.fromJson(serialized, new TypeToken<HashMap<String, Object>>(){}.getType());
        assertThat(parsedRaw.get("pf"), instanceOf(List.class));
        List<Object> asList = (ArrayList) parsedRaw.get("pf");
        assertThat(asList.size(), is(equalTo(1)));
        Map<String, Object> item0 = (Map<String, Object>) asList.get(0);
        assertThat(item0.get("f"), is(equalTo("test1")));
        assertThat(item0.get("m"), is(equalTo(0.0)));
        assertThat(item0.get("rc"), is(equalTo(23.0)));
    }
}
