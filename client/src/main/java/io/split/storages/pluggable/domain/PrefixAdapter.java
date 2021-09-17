package io.split.storages.pluggable.domain;

import java.util.ArrayList;
import java.util.List;

public class PrefixAdapter {

    private static final String DEFAULT_PREFIX = "SPLITIO.";
    private static final String SPLIT_PREFIX = "split.";
    private static final String SPLITS_PREFIX = "splits.";
    private static final String TRAFFIC_TYPE_PREFIX = "trafficType.";
    private static final String EVENTS = "events";
    private static final String IMPRESSIONS = "impressions";
    private static final String SEGMENT = "segment.";
    private static final String TILL = "till";
    private static final String TELEMETRY = "telemetry.";
    private static final String LATENCIES = "latencies";
    private static final String EXCEPTIONS = "exceptions";
    private static final String INIT = "init";

    public static String buildSplitKey(String name) {
        return String.format(DEFAULT_PREFIX+ SPLIT_PREFIX +"{%s}", name);
    }

    public static String buildSplitChangeNumber() {
        return DEFAULT_PREFIX+SPLITS_PREFIX+"till";
    }

    public static String buildGetAllSplit() {
        return DEFAULT_PREFIX+SPLITS_PREFIX+"*";
    }

    public static String buildTrafficTypeExists(String trafficType) {
        return String.format(DEFAULT_PREFIX+TRAFFIC_TYPE_PREFIX+"{%s}", trafficType);
    }

    public static List<String> buildFetchManySplits(List<String> names) {
        List<String> prefixes = new ArrayList<>();
        for(String name : names) {
            prefixes.add(String.format(DEFAULT_PREFIX+ SPLIT_PREFIX +"{%s}", name));
        }
        return prefixes;
    }

    public static String buildEvent() {
        return DEFAULT_PREFIX+EVENTS;
    }

    public static String buildImpressions() {
        return DEFAULT_PREFIX+IMPRESSIONS;
    }

    public static String buildSegment(String segmentName) {
        return String.format(DEFAULT_PREFIX+SEGMENT+"{%s}", segmentName);
    }

    public static String buildSegmentAll() {
        return String.format(DEFAULT_PREFIX+SEGMENT+"*");
    }

    public static String buildSegmentTill(String segmentName) {
        return String.format(DEFAULT_PREFIX+SEGMENT+"{%s}."+TILL, segmentName);
    }

    public static String buildTelemetryLatenciesPrefix(String method, int bucketForLatency, String sdkVersion, String machineIp, String machineName) {
        return String.format(DEFAULT_PREFIX+TELEMETRY+LATENCIES+"::{%s}/{%s}/{%s}/"+"{%s}/{%d}", sdkVersion, machineName, machineIp, method, bucketForLatency);
    }

    public static String buildTelemetryExceptionsPrefix(String method, String sdkVersion, String machineIp, String machineName) {
        return String.format(DEFAULT_PREFIX+TELEMETRY+EXCEPTIONS+"::{%s}/{%s}/{%s}/"+"{%s}", sdkVersion, machineName, machineIp, method);
    }

    public static String buildTelemetryInit(String sdkVersion, String machineIp, String machineName) {
        return String.format(DEFAULT_PREFIX+TELEMETRY+INIT+"::{%s}/{%s}/{%s}", sdkVersion, machineName, machineIp);
    }
}
