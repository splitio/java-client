package io.split.api;

import io.split.api.client.SplitApiClient;
import io.split.api.client.api.ApiClient;
import io.split.api.client.local.LocalApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

public class SplitApiFactory {
    private static final Logger _log = LoggerFactory.getLogger(SplitApiFactory.class);

    public static SplitApiFactory build(String apiToken) throws IOException, InterruptedException, TimeoutException, URISyntaxException {
        return build(apiToken, SplitClientConfig.builder().build());
    }

    public static synchronized SplitApiClient build(String apiToken, SplitClientConfig config) throws IOException, InterruptedException, TimeoutException, URISyntaxException {
//        if (LocalhostSplitFactory.LOCALHOST.equals(apiToken)) {
//            return LocalhostSplitFactory.createLocalhostSplitFactory();
//        }
//
//        RequestConfig requestConfig = RequestConfig.custom()
//                .setConnectTimeout(config.connectionTimeout())
//                .setSocketTimeout(config.readTimeout())
//                .build();
//`
//        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
//        cm.setMaxTotal(20);
//        cm.setDefaultMaxPerRoute(20);
//        final CloseableHttpClient httpclient = HttpClients.custom()
//                .setConnectionManager(cm)
//                .setDefaultRequestConfig(requestConfig)
//                .addInterceptorLast(AddSplitHeadersFilter.instance(apiToken))
//                .addInterceptorLast(new GzipEncoderRequestInterceptor())
//                .addInterceptorLast(new GzipDecoderResponseInterceptor())
//                .build();
//
//        URI rootTarget = URI.create(config.endpoint());
//        URI eventsRootTarget = URI.create(config.eventsEndpoint());
//

        return new ApiClient();
    }

    public static SplitApiClient local() throws IOException {
        return new LocalApiClient();
    }
}
