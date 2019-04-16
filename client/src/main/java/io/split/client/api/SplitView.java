package io.split.client.api;

import java.util.List;
import java.util.Map;

/**
 * A view of a Split meant for consumption through SplitManager interface.
 *
 * @author adil
 */
public class SplitView {
    public String name;
    public String trafficType;
    public boolean killed;
    public List<String> treatments;
    public long changeNumber;
    public Map<String, String> configs;
}
