package io.split.client.dtos;

public class ExcludedSegments {
    static final String STANDARD_TYPE = "standard";
    static final String RULE_BASED_TYPE = "rule-based";

    public ExcludedSegments() {}
    public ExcludedSegments(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String type;
    public String name;

    public boolean isStandard() {
        return STANDARD_TYPE.equals(type);
    }

    public boolean isRuleBased() {
        return RULE_BASED_TYPE.equals(type);
    }

    public String getSegmentName(){
        return name;
    }
}
