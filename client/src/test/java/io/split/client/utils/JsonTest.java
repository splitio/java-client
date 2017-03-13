package io.split.client.utils;

import io.split.client.dtos.Partition;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by adilaijaz on 6/13/16.
 */
public class JsonTest {
    @Test
    public void unknownFieldsDontCauseProblems() {

        String json = "{\"treatment\":\"on\", \"size\":20, \"foo\":\"bar\"}";
        Partition partition = Json.fromJson(json, Partition.class);

        assertThat(partition.treatment, is(equalTo("on")));
        assertThat(partition.size, is(equalTo(20)));
    }
}
