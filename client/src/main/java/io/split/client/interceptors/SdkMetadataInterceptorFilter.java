package io.split.client.interceptors;

import io.split.client.SplitClientConfig;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;

public class SdkMetadataInterceptorFilter implements HttpRequestInterceptor {
    private static final Logger _log = LoggerFactory.getLogger(SdkMetadataInterceptorFilter.class);

    static final String CLIENT_MACHINE_NAME_HEADER = "SplitSDKMachineName";
    static final String CLIENT_MACHINE_IP_HEADER = "SplitSDKMachineIP";
    static final String CLIENT_VERSION = "SplitSDKVersion";

    private final String _hostname;
    private final String _ip;
    private final String _sdkVersion;

    public static SdkMetadataInterceptorFilter instance(boolean ipAddressEnabled, String sdkVersion) {
        String hostName = null;
        String ip = null;

        if (ipAddressEnabled) {
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                hostName = localHost.getHostName();
                ip = localHost.getHostAddress();
            } catch (Exception e) {
                _log.error("Could not resolve InetAddress", e);
            }
        }

        return new SdkMetadataInterceptorFilter(hostName, ip, sdkVersion);
    }

    private SdkMetadataInterceptorFilter(String hostName, String ip, String sdkVersion) {
        _sdkVersion = sdkVersion;
        _hostname = hostName;
        _ip = ip;
    }

    @Override
    public void process(HttpRequest httpRequest, EntityDetails entityDetails, HttpContext httpContext) throws HttpException, IOException {
        httpRequest.addHeader(CLIENT_VERSION, SplitClientConfig.splitSdkVersion);

        if (_hostname != null) {
            httpRequest.addHeader(CLIENT_MACHINE_NAME_HEADER, _hostname);
        }

        if (_ip != null) {
            httpRequest.addHeader(CLIENT_MACHINE_IP_HEADER, _ip);
        }
    }
}
