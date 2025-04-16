package io.split.client.dtos;

import com.google.gson.annotations.SerializedName;

public class SplitChange {
    @SerializedName("ff")
    public ChangeDto<Split> featureFlags;
    @SerializedName("rbs")
    public ChangeDto<RuleBasedSegment> ruleBasedSegments;
}
