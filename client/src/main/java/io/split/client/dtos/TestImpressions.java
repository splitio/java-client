package io.split.client.dtos;

import java.util.List;
import java.util.stream.Collectors;

public class TestImpressions {
    public String testName;
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
