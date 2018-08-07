package io.split.client.impressions;

import io.split.client.dtos.TestImpressions;
import io.split.client.utils.Utils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by patricioe on 6/20/16.
 */
public class HttpImpressionsSender implements ImpressionsSender {

    private static final Logger _logger = LoggerFactory.getLogger(HttpImpressionsSender.class);

    private CloseableHttpClient _client;
    private URI _eventsEndpoint;


    public static HttpImpressionsSender create(CloseableHttpClient clinent, URI eventsRootEndpoint) throws URISyntaxException {
        return new HttpImpressionsSender(clinent, Utils.appendPath(eventsRootEndpoint, "/api/testImpressions/bulk"));
    }

    private HttpImpressionsSender(CloseableHttpClient client, URI eventsEndpoint) throws URISyntaxException {
        _client = client;
        _eventsEndpoint = eventsEndpoint;
    }

    @Override
    public void post(List<TestImpressions> impressions) {

        CloseableHttpResponse response = null;

        try {
            StringEntity entity = Utils.toJsonEntity(impressions);

            HttpPost request = new HttpPost(_eventsEndpoint);
            request.setEntity(entity);

            response = _client.execute(request);

            int status = response.getStatusLine().getStatusCode();

            if (status < 200 || status >= 300) {
                _logger.warn("Response status was: " + status);
            }

        } catch (Throwable t) {
            _logger.warn("Exception when posting impressions" + impressions, t);
        } finally {
            Utils.forceClose(response);
        }

    }

}
