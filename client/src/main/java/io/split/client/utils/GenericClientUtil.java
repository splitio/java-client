package io.split.client.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.split.client.dtos.RuleBasedSegment;
import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class GenericClientUtil {

    private static final Logger _log = LoggerFactory.getLogger(GenericClientUtil.class);

    public static<T> void process(List<T> data, URI endpoint, CloseableHttpClient client) {
        CloseableHttpResponse response = null;

        try {
            HttpEntity entity = Utils.toJsonEntity(data);

            HttpPost request = new HttpPost(endpoint);
            request.setEntity(entity);

            response = client.execute(request);

            int status = response.getCode();

            if (status < 200 || status >= 300) {
                _log.info(String.format("Posting %d records returned with status: %d", data.size(), status));
            }

        } catch (Throwable t) {
            if (_log.isDebugEnabled()) {
                _log.debug(String.format("Posting %d records returned with error", data.size()), t);
            }
        } finally {
            Utils.forceClose(response);
        }

    }

    public static SplitChange ExtractFeatureFlagsAndRuleBasedSegments(String responseBody) {
        JsonObject jsonBody = Json.fromJson(responseBody, JsonObject.class);
        JsonObject featureFlags = jsonBody.getAsJsonObject("ff");
        JsonObject ruleBasedSegments = jsonBody.getAsJsonObject("rbs");
        SplitChange splitChange = new SplitChange();
        splitChange.till = Long.parseLong(featureFlags.get("t").toString());
        splitChange.since = Long.parseLong(featureFlags.get("s").toString());
        splitChange.tillRBS = Long.parseLong(ruleBasedSegments.get("t").toString());
        splitChange.sinceRBS = Long.parseLong(ruleBasedSegments.get("s").toString());

        splitChange.splits = new ArrayList<>();
        for (JsonElement split: featureFlags.get("d").getAsJsonArray()) {
            splitChange.splits.add(Json.fromJson(split.toString(), Split.class));
        }
        splitChange.ruleBasedSegments = new ArrayList<>();
        for (JsonElement rbs: ruleBasedSegments.get("d").getAsJsonArray()) {
            splitChange.ruleBasedSegments.add(Json.fromJson(rbs.toString(), RuleBasedSegment.class));
        }
        return splitChange;
    }
}
