package io.split.client.dtos.pluggable;

import com.google.gson.annotations.SerializedName;
import io.split.client.dtos.KeyImpression;
import io.split.client.dtos.Metadata;
import io.split.client.dtos.TestImpressions;

import java.util.List;

public class ImpressionsPluggable extends TestImpressions {

    /* package private */ static final String FIELD_METADATA = "m";

    @SerializedName(FIELD_METADATA)
    private final Metadata _metadata;

    public ImpressionsPluggable(Metadata metadata, String testName, List<KeyImpression> keyImpressions) {
        super(testName, keyImpressions);
        _metadata = metadata;
    }

    public Metadata get_metadata() {
        return _metadata;
    }
}
