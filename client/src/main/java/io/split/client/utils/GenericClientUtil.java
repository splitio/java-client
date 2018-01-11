package io.split.client.utils;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

public class GenericClientUtil {

    private static final Logger _log = LoggerFactory.getLogger(GenericClientUtil.class);

    public static<T> void process(List<T> data, URI endpoint, CloseableHttpClient client) {
        CloseableHttpResponse response = null;

        try {
            StringEntity entity = Utils.toJsonEntity(data);

            HttpPost request = new HttpPost(endpoint);
            request.setEntity(entity);

            response = client.execute(request);

            int status = response.getStatusLine().getStatusCode();

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
}
