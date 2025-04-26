package io.split.client.dtos;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class SplitChangesOldPayloadDto {
    @SerializedName("since")
    public long s;

    @SerializedName("till")
    public long t;

    @SerializedName("splits")
    public List<Split> d;

    public SplitChange toSplitChange() {
        SplitChange splitChange = new SplitChange();
        ChangeDto<Split> ff = new ChangeDto<>();
        ff.s = this.s;
        ff.t = this.t;
        ff.d = this.d;
        ChangeDto<RuleBasedSegment> rbs = new ChangeDto<>();
        rbs.d = new ArrayList<>();
        rbs.t = -1;
        rbs.s = -1;
        
        splitChange.featureFlags = ff;
        splitChange.ruleBasedSegments = rbs;
        
        return splitChange;
    }
}
