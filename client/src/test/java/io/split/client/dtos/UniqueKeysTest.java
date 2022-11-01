package io.split.client.dtos;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

public class UniqueKeysTest {

    @Test
    public void TestShrinkedPropertyNames() {
        Gson gson = new Gson();
        List<String> keys = new ArrayList<>();
        keys.add("key-1");
        keys.add("key-2");
        List<UniqueKeys.UniqueKey> uniqueKeys = new ArrayList<>();
        uniqueKeys.add(new UniqueKeys.UniqueKey("feature-1", keys));
        UniqueKeys imp = new UniqueKeys(uniqueKeys);
        String serialized = gson.toJson(imp);

        HashMap<String, Object> parsedRaw = gson.fromJson(serialized, new TypeToken<HashMap<String, Object>>(){}.getType());
        assertThat(parsedRaw.get("keys"), instanceOf(List.class));
        List<Object> asList = (ArrayList) parsedRaw.get("keys");
        assertThat(asList.size(), is(equalTo(1)));

        Map<String, Object> item0 = (Map<String, Object>) asList.get(0);
        assertThat(item0.get("f"), is(equalTo("feature-1")));

        List<String> ks = (List<String>) item0.get("ks");
        assertThat(ks.get(0), is(equalTo("key-1")));
        assertThat(ks.get(1), is(equalTo("key-2")));
    }
}