package io.split.client.interceptors;

import io.split.client.SplitClientConfig;
import io.split.client.utils.SDKMetadata;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SdkMetadataInterceptorFilter implements HttpRequestInterceptor {
    private static final Logger _log = LoggerFactory.getLogger(SdkMetadataInterceptorFilter.class);

    static final String CLIENT_MACHINE_NAME_HEADER = "SplitSDKMachineName";
    static final String CLIENT_MACHINE_IP_HEADER = "SplitSDKMachineIP";
    static final String CLIENT_VERSION = "SplitSDKVersion";

    private final String _hostname;
    private final String _ip;
    private final String _sdkVersion;

    public static SdkMetadataInterceptorFilter instance(SDKMetadata sdkMetadata) {
        return new SdkMetadataInterceptorFilter(sdkMetadata.getMachineName(), sdkMetadata.getMachineIp(), sdkMetadata.getMachineName());
    }

    private SdkMetadataInterceptorFilter(String hostName, String ip, String sdkVersion) {
        _sdkVersion = sdkVersion;
        _hostname = hostName;
        _ip = ip;
    }

    @Override
    public void process(HttpRequest httpRequest, EntityDetails entityDetails, HttpContext httpContext) throws HttpException, IOException {
        httpRequest.addHeader(CLIENT_VERSION, SplitClientConfig.splitSdkVersion);

        if (!"".equals(_hostname)) {
            httpRequest.addHeader(CLIENT_MACHINE_NAME_HEADER, _hostname);
        }

        if (!"".equals(_ip)) {
            httpRequest.addHeader(CLIENT_MACHINE_IP_HEADER, _ip);
        }
    }
}
