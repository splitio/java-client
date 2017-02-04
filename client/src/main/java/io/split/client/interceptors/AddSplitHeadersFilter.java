package io.split.client.interceptors;

import io.split.client.SplitClientConfig;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by adilaijaz on 5/22/15.
 */
public class AddSplitHeadersFilter implements HttpRequestInterceptor {
    private static final Logger _log = LoggerFactory.getLogger(AddSplitHeadersFilter.class);

    private static final String CLIENT_MACHINE_NAME_HEADER = "SplitSDKMachineName";
    private static final String CLIENT_MACHINE_IP_HEADER = "SplitSDKMachineIP";
    private static final String CLIENT_VERSION = "SplitSDKVersion";
    private static final String SDK_SPEC_VERSION = "SplitSDKSpecVersion";
    private static final String OUR_SDK_SPEC_VERSION = "1.3";

    private final String _apiTokenBearer;
    private final String _hostname;
    private final String _ip;

    public static AddSplitHeadersFilter instance(String apiToken) {
        String hostname = null;
        String ip = null;

        try {
            InetAddress localHost = InetAddress.getLocalHost();
            hostname = localHost.getHostName();
            ip = localHost.getHostAddress();
        } catch (Exception e) {
            _log.error("Could not resolve InetAddress", e);
        }

        return new AddSplitHeadersFilter(apiToken, hostname, ip);
    }

    private AddSplitHeadersFilter(String apiToken, String hostname, String ip) {
        checkNotNull(apiToken);

        _apiTokenBearer = "Bearer " + apiToken;
        _hostname = hostname;
        _ip = ip;
    }

    @Override
    public void process(HttpRequest request, HttpContext httpContext) throws HttpException, IOException {
        request.addHeader("Authorization", _apiTokenBearer);
        request.addHeader(CLIENT_VERSION, SplitClientConfig.splitSdkVersion);
        ;
        request.addHeader(SDK_SPEC_VERSION, OUR_SDK_SPEC_VERSION);

        if (_hostname != null) {
            request.addHeader(CLIENT_MACHINE_NAME_HEADER, _hostname);
        }

        if (_ip != null) {
            request.addHeader(CLIENT_MACHINE_IP_HEADER, _ip);
        }

    }
}
