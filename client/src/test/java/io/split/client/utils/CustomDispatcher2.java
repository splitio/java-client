package io.split.client.utils;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;

public class CustomDispatcher2 extends Dispatcher {
    public static final String SPLIT_FETCHER_1 = "/api/splitChanges?s=1.3&since=-1&rbSince=-1";
    public static final String SPLIT_FETCHER_2 = "/api/splitChanges?s=1.3&since=1675095324253&rbSince=1585948850111";
    public static final String SPLIT_FETCHER_3 = "/api/splitChanges?s=1.3&since=1685095324253&rbSince=1585948850111";
    public static final String SPLIT_FETCHER_4 = "/api/splitChanges?s=1.3&since=1695095324253&rbSince=1585948850111";
    public static final String SPLIT_FETCHER_5 = "/api/splitChanges?s=1.3&since=1775095324253&rbSince=1585948850111";

    private final Map<String, Queue<MockResponse>>_responses;

    public CustomDispatcher2(Map<String, Queue<MockResponse>> responses){
        _responses = responses;
    }

    public static CustomDispatcher2.Builder builder() {
        return new CustomDispatcher2.Builder();
    }

    MockResponse response = new MockResponse().setBody("{" +
            "\"ff\":{" +
            "\"t\":1675095324253," +
            "\"s\":-1," +
            "\"d\": [{" +
            "\"changeNumber\": 123," +
            "\"trafficTypeName\": \"user\"," +
            "\"name\": \"some_name\"," +
            "\"trafficAllocation\": 100," +
            "\"trafficAllocationSeed\": 123456," +
            "\"seed\": 321654," +
            "\"status\": \"ACTIVE\"," +
            "\"killed\": false," +
            "\"defaultTreatment\": \"off\"," +
            "\"algo\": 2," +
            "\"conditions\": [" +
            "{" +
            "\"partitions\": [" +
            "{\"treatment\": \"on\", \"size\": 50}," +
            "{\"treatment\": \"off\", \"size\": 50}" +
            "]," +
            "\"contitionType\": \"WHITELIST\"," +
            "\"label\": \"some_label\"," +
            "\"matcherGroup\": {" +
            "\"matchers\": [" +
            "{" +
            "\"matcherType\": \"WHITELIST\"," +
            "\"whitelistMatcherData\": {" +
            "\"whitelist\": [\"k1\", \"k2\", \"k3\"]" +
            "}," +
            "\"negate\": false" +
            "}" +
            "]," +
            "\"combiner\": \"AND\"" +
            "}" +
            "}," +
            "{" +
            "\"conditionType\": \"ROLLOUT\"," +
            "\"matcherGroup\": {" +
            "\"combiner\": \"AND\"," +
            "\"matchers\": [" +
            "{" +
            "\"keySelector\": {" +
            "\"trafficType\": \"user\"" +
            "}," +
            "\"matcherType\": \"IN_RULE_BASED_SEGMENT\"," +
            "\"negate\": false," +
            "\"userDefinedSegmentMatcherData\": {" +
            "\"segmentName\": \"sample_rule_based_segment\"" +
            "}" +
            "}" +
            "]" +
            "}," +
            "\"partitions\": [" +
            "{" +
            "\"treatment\": \"on\"," +
            "\"size\": 100" +
            "}," +
            "{" +
            "\"treatment\": \"off\"," +
            "\"size\": 0" +
            "}" +
            "]," +
            "\"label\": \"in rule based segment sample_rule_based_segment\"" +
            "}" +
            "]," +
            "\"sets\": [\"set1\", \"set2\"]}]" +
            "}," +
            "\"rbs\":  {" +
            "\"t\": 1585948850111," +
            "\"s\": -1," +
            "\"d\": [" +
            "{" +
            "\"changeNumber\": 5," +
            "\"name\": \"sample_rule_based_segment\"," +
            "\"status\": \"ACTIVE\"," +
            "\"trafficTypeName\": \"user\"," +
            "\"excluded\":{" +
            "\"keys\":[\"mauro@split.io\",\"gaston@split.io\"]," +
            "\"segments\":[]" +
            "}," +
            "\"conditions\": [" +
            "{" +
            "\"matcherGroup\": {" +
            "\"combiner\": \"AND\"," +
            "\"matchers\": [" +
            "{" +
            "\"keySelector\": {" +
            "\"trafficType\": \"user\"," +
            "\"attribute\": \"email\"" +
            "}," +
            "\"matcherType\": \"ENDS_WITH\"," +
            "\"negate\": false," +
            "\"whitelistMatcherData\": {" +
            "\"whitelist\": [" +
            "\"@split.io\"" +
            "]}}]}}]}]}}");
    MockResponse response2 = new MockResponse().setBody("{\"ff\":{\"d\": [], \"s\":1675095324253, \"t\":1675095324253}, \"rbs\":{\"d\":[],\"s\":1585948850111,\"t\":1585948850111}}");
    MockResponse response3 = new MockResponse().setBody("{\"ff\":{\"d\": [], \"s\":1685095324253, \"t\":1695095324253}, \"rbs\":{\"d\":[],\"s\":1585948850111,\"t\":1585948850111}}");
    MockResponse response4 = new MockResponse().setBody("{\"ff\":{\"d\": [], \"s\":1695095324253, \"t\":1775095324253}, \"rbs\":{\"d\":[],\"s\":1585948850111,\"t\":1585948850111}}");
    MockResponse response5 = new MockResponse().setBody("{\"ff\":{\"d\": [], \"s\":1775095324253, \"t\":1775095324253}, \"rbs\":{\"d\":[],\"s\":1585948850111,\"t\":1585948850111}}");

    @NotNull
    @Override
    public MockResponse dispatch(@NotNull RecordedRequest request) {
        switch (request.getPath()) {
            case CustomDispatcher2.SPLIT_FETCHER_1:
                return getResponse(CustomDispatcher2.SPLIT_FETCHER_1, response);
            case CustomDispatcher2.SPLIT_FETCHER_2:
                return getResponse(CustomDispatcher2.SPLIT_FETCHER_2, response2);
            case CustomDispatcher2.SPLIT_FETCHER_3:
                return getResponse(CustomDispatcher2.SPLIT_FETCHER_3, response3);
            case CustomDispatcher2.SPLIT_FETCHER_4:
                return getResponse(CustomDispatcher2.SPLIT_FETCHER_4, response4);
            case CustomDispatcher2.SPLIT_FETCHER_5:
                return getResponse(CustomDispatcher2.SPLIT_FETCHER_5, response5);
        }
        return new MockResponse().setResponseCode(404);
    }

    private MockResponse getResponse(String target, MockResponse mockedResponse) {
        Queue<MockResponse> responses = _responses.get(target);
        if(responses != null) {
            MockResponse finalResponse = responses.poll();
            return finalResponse == null ? mockedResponse : finalResponse;
        }
        return mockedResponse;
    }



    public static final class Builder {
        private Map<String, Queue<MockResponse>> _responses = new HashMap<>();
        public Builder(){};

        /**
         * Add responses to an specific path
         * @param path
         * @param responses
         * @return
         */
        public Builder path(String path, Queue<MockResponse> responses) {
            _responses.put(path, responses);
            return this;
        }

        public CustomDispatcher2 build() {
            return new CustomDispatcher2(_responses);
        }
    }
}
