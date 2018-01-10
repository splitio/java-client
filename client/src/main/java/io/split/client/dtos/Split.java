package io.split.client.dtos;

import java.util.List;

public class Split {
    public String name;
    public int seed;
    public Status status;
    public boolean killed;
    public String defaultTreatment;
    public List<Condition> conditions;
    public String trafficTypeName;
    public long changeNumber;
    public Integer trafficAllocation;
    public Integer trafficAllocationSeed;
    public int algo;


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
