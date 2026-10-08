package io.split.client.lifecycle;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stateful mock Split backend for the lifecycle event tests. Every endpoint answers "nothing changed" unless a
 * response was scripted for the change number being requested, which lets a test publish exactly one change at
 * the moment it chooses.
 */
final class LifecycleScriptDispatcher extends Dispatcher {

    static final String SEGMENT = "seg1";

    private static final String AUTH_BODY =
            "{\"pushEnabled\":true,\"token\":\"khdkjdahs987498217.eyJ4LWFibHktY2FwYWJpbGl0eSI6IntcInh4eHhfeHh4eF9zZWdtZW50c1wiOltcInN1YnNjcmliZVwiXSxcInh4eHhfeHh4eF9zcGxpdHNcIjpbXCJzdWJzY3JpYmVcIl0sXCJjb250cm9sXCI6W1wic3Vic2NyaWJlXCJdfSIsImV4cCI6MTU5MzAyNzQxMCwiaWF0IjoxNTkzMDIzODEwfQ\"}";

    private final String initialFlags;
    private final String initialSegment;
    private final long headersDelayMillis;
    private final Map<Long, String> flagScripts = new ConcurrentHashMap<>();
    private final Map<Long, String> segmentScripts = new ConcurrentHashMap<>();
    private final AtomicLong flagRequests = new AtomicLong();

    LifecycleScriptDispatcher(String initialFlags, String initialSegment, long headersDelayMillis) {
        this.initialFlags = initialFlags;
        this.initialSegment = initialSegment;
        this.headersDelayMillis = headersDelayMillis;
    }

    /** Body served the next time the SDK asks for flag changes since {@code since}. */
    void scriptFlags(long since, String body) {
        flagScripts.put(since, body);
    }

    /** Body served the next time the SDK asks for changes of {@link #SEGMENT} since {@code since}. */
    void scriptSegment(long since, String body) {
        segmentScripts.put(since, body);
    }

    long flagRequests() {
        return flagRequests.get();
    }

    @Override
    public MockResponse dispatch(RecordedRequest request) {
        String path = request.getPath();
        if (path.startsWith("/api/auth")) {
            return new MockResponse().setBody(AUTH_BODY);
        }
        if (path.startsWith("/api/splitChanges")) {
            flagRequests.incrementAndGet();
            long since = Long.parseLong(request.getRequestUrl().queryParameter("since"));
            long rbSince = Long.parseLong(request.getRequestUrl().queryParameter("rbSince"));
            MockResponse response = new MockResponse();
            if (headersDelayMillis > 0) {
                response.setHeadersDelay(headersDelayMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
            if (since == -1) {
                return response.setBody(initialFlags);
            }
            String scripted = flagScripts.get(since);
            return response.setBody(scripted != null ? scripted : noFlagChanges(since, rbSince));
        }
        if (path.startsWith("/api/segmentChanges/")) {
            long since = Long.parseLong(request.getRequestUrl().queryParameter("since"));
            if (since == -1) {
                return new MockResponse().setBody(initialSegment);
            }
            String scripted = segmentScripts.get(since);
            return new MockResponse().setBody(scripted != null ? scripted : noSegmentChanges(since));
        }
        // telemetry, impressions, events
        return new MockResponse().setResponseCode(200);
    }

    static String noFlagChanges(long since, long rbSince) {
        return "{\"ff\":{\"d\":[],\"s\":" + since + ",\"t\":" + since + "},\"rbs\":{\"d\":[],\"s\":" + rbSince
                + ",\"t\":" + rbSince + "}}";
    }

    static String noSegmentChanges(long since) {
        return segmentChange(since, since, "", "");
    }

    static String segmentChange(long since, long till, String addedCsv, String removedCsv) {
        return "{\"name\":\"" + SEGMENT + "\",\"added\":[" + quoted(addedCsv) + "],\"removed\":[" + quoted(removedCsv)
                + "],\"since\":" + since + ",\"till\":" + till + "}";
    }

    /** Flags body for a first fetch (since=-1) or a scripted change; {@code rbs} may be empty. */
    static String flagsBody(long since, long till, String flagsJson, long rbSince, long rbTill, String rbsJson) {
        return "{\"ff\":{\"d\":[" + flagsJson + "],\"s\":" + since + ",\"t\":" + till + "},\"rbs\":{\"d\":["
                + rbsJson + "],\"s\":" + rbSince + ",\"t\":" + rbTill + "}}";
    }

    static String flag(String name, long changeNumber, boolean inSegment) {
        String matcher = inSegment
                ? "{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":null},\"matcherType\":\"IN_SEGMENT\","
                    + "\"negate\":false,\"userDefinedSegmentMatcherData\":{\"segmentName\":\"" + SEGMENT + "\"}}"
                : "{\"keySelector\":{\"trafficType\":\"user\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\","
                    + "\"negate\":false}";
        return "{\"trafficTypeName\":\"user\",\"name\":\"" + name + "\",\"trafficAllocation\":100,"
                + "\"trafficAllocationSeed\":-2092979940,\"seed\":105482719,\"status\":\"ACTIVE\",\"killed\":false,"
                + "\"defaultTreatment\":\"off\",\"changeNumber\":" + changeNumber + ",\"algo\":2,\"configurations\":{},"
                + "\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":["
                + matcher + "]},\"partitions\":[{\"treatment\":\"on\",\"size\":100},{\"treatment\":\"off\",\"size\":0}],"
                + "\"label\":\"default rule\"}]}";
    }

    static String ruleBasedSegment(String name, long changeNumber) {
        return "{\"changeNumber\":" + changeNumber + ",\"name\":\"" + name + "\",\"status\":\"ACTIVE\","
                + "\"trafficTypeName\":\"user\",\"excluded\":{\"keys\":[],\"segments\":[]},\"conditions\":["
                + "{\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"user\","
                + "\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false}]}}]}";
    }

    private static String quoted(String csv) {
        if (csv.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String item : csv.split(",")) {
            if (out.length() > 0) {
                out.append(',');
            }
            out.append('"').append(item).append('"');
        }
        return out.toString();
    }
}
