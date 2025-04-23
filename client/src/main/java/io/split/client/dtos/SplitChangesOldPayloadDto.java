package io.split.client.dtos;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SplitChangesOldPayloadDto {
    @SerializedName("since")
    public long s;

    @SerializedName("till")
    public long t;

    @SerializedName("splits")
    public List<Split> d;

    public ChangeDto<Split> toChangeDTO() {
        ChangeDto<Split> dto = new ChangeDto<>();
        dto.s = this.s;
        dto.t = this.t;
        dto.d = this.d;
        return dto;

    }
}
