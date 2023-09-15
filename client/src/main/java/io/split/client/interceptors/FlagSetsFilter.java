package io.split.client.interceptors;

import java.util.HashSet;

public interface FlagSetsFilter {

    boolean Intersect(HashSet<String> sets);
    boolean Intersect(String set);
}