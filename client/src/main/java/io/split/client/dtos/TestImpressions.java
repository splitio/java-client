package io.split.client.dtos;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.stream.Collectors;

public class TestImpressions {

    /* package private */ static final String FIELD_TEST_NAME = "f";
    /* package private */ static final String FIELD_KEY_IMPRESSIONS = "i";

    @SerializedName(FIELD_TEST_NAME)
    public String testName;

    @SerializedName(FIELD_KEY_IMPRESSIONS)
    public List<KeyImpression> keyImpressions;

    public TestImpressions(String testName_, List<KeyImpression> keyImpressions_) {
        testName = testName_;
        keyImpressions = keyImpressions_;
    }

    public static List<TestImpressions> fromKeyImpressions(List<KeyImpression> impressions) {
        return impressions.stream()
                .collect(Collectors.groupingBy(ki -> ki.feature))
                .entrySet().stream()
                .map((e) -> new TestImpressions(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }
}
