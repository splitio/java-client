package io.split.client.dtos;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Prerequisites {
    @SerializedName("n")
    public String featureFlagName;
    @SerializedName("ts")
    public List<String> treatments;
}
