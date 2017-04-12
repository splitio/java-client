package io.split.client.impressions;

import io.split.client.dtos.TestImpressions;

import java.util.List;

/**
 * Created by patricioe on 6/20/16.
 */
public interface ImpressionsSender {

    void post(List<TestImpressions> impressions);
}
