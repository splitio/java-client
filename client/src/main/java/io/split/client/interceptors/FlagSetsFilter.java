package io.split.client.interceptors;

import java.util.Set;

public interface FlagSetsFilter {

    boolean Intersect(Set<String> sets);
    boolean Intersect(String set);
}