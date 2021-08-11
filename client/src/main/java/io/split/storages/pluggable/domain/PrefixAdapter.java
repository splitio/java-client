package io.split.storages.pluggable.domain;

import java.util.ArrayList;
import java.util.List;

public class PrefixAdapter {

    private static final String DEFAULT_PREFIX = "SPLITIO.";
    private static final String SPLIT_PREFIX = "split.";
    private static final String SPLITS_PREFIX = "splits.";
    private static final String TRAFFIC_TYPE_PREFIX = "trafficType.";
    private static final String EVENTS = "events";

    //Split Consumer
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

}
