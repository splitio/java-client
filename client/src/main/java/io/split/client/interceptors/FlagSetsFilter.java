package io.split.client.interceptors;

import java.util.Set;

public interface FlagSetsFilter {

    boolean intersect(Set<String> sets);
    boolean intersect(String set);
}