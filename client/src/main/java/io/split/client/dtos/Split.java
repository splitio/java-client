package io.split.client.dtos;

import java.util.List;
import java.util.Map;

public class Split {
    public String name;
    public Integer seed;
    public Status status;
    public Boolean killed;
    public String defaultTreatment;
    public List<Condition> conditions;
    public String trafficTypeName;
    public Long changeNumber;
    public Integer trafficAllocation;
    public Integer trafficAllocationSeed;
    public Integer algo;
    public Map<String, String> configurations;


    @Override
    public String toString() {
        return "Split{" +
                "name='" + name + '\'' +
                ", status=" + status +
                ", trafficTypeName='" + trafficTypeName + '\'' +
                ", changeNumber=" + changeNumber +
                '}';
    }
}
