package io.split.service;

import io.split.client.RequestDecorator;
import io.split.client.dtos.SplitHttpResponse;
import io.split.client.utils.SDKMetadata;
import io.split.engine.common.FetchOptions;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SplitHttpClientKerberosImpl implements SplitHttpClient {

    private static final Logger _log = LoggerFactory.getLogger(SplitHttpClient.class);
    private static final String HEADER_CACHE_CONTROL_NAME = "Cache-Control";
    private static final String HEADER_CACHE_CONTROL_VALUE = "no-cache";
    private static final String HEADER_API_KEY = "Authorization";
    private static final String HEADER_CLIENT_KEY = "SplitSDKClientKey";
    private static final String HEADER_CLIENT_MACHINE_NAME = "SplitSDKMachineName";
    private static final String HEADER_CLIENT_MACHINE_IP = "SplitSDKMachineIP";
    private static final String HEADER_CLIENT_VERSION = "SplitSDKVersion";

    private final RequestDecorator _requestDecorator;
    private final String _apikey;
    private final SDKMetadata _metadata;

    public static SplitHttpClientKerberosImpl create(RequestDecorator requestDecorator,
                                                     String apikey,
                                                     SDKMetadata metadata) throws URISyntaxException {
        return new SplitHttpClientKerberosImpl(requestDecorator, apikey, metadata);
    }

    SplitHttpClientKerberosImpl(RequestDecorator requestDecorator,
                                String apikey,
                                SDKMetadata metadata) {
        _requestDecorator = requestDecorator;
        _apikey = apikey;
        _metadata = metadata;
    }

    public synchronized SplitHttpResponse get(URI uri, FetchOptions options, Map<String, List<String>> additionalHeaders) {
        HttpURLConnection getHttpURLConnection = null;
        try {
            getHttpURLConnection = (HttpURLConnection) uri.toURL().openConnection();
            return _get(getHttpURLConnection, options, additionalHeaders);
        } catch  (Exception e) {
            throw new IllegalStateException(String.format("Problem in http get operation: %s", e), e);
        } finally {
            try {
                if (getHttpURLConnection != null) {
                    getHttpURLConnection.disconnect();
                }
            } catch (Exception e) {
                _log.error(String.format("Could not close HTTP URL Connection: %s", e), e);
            }
        }
    }
    public SplitHttpResponse _get(HttpURLConnection getHttpURLConnection, FetchOptions options, Map<String, List<String>> additionalHeaders) {
        InputStreamReader inputStreamReader = null;
        try {
            getHttpURLConnection.setRequestMethod("GET");

            setBasicHeaders(getHttpURLConnection);
            setAdditionalAndDecoratedHeaders(getHttpURLConnection, additionalHeaders);

            if (options.cacheControlHeadersEnabled()) {
                getHttpURLConnection.setRequestProperty(HEADER_CACHE_CONTROL_NAME, HEADER_CACHE_CONTROL_VALUE);
            }

            _log.debug(String.format("Request Headers: %s", getHttpURLConnection.getRequestProperties()));

            int responseCode = getHttpURLConnection.getResponseCode();

            if (_log.isDebugEnabled()) {
                _log.debug(String.format("[%s] %s. Status code: %s",
                        getHttpURLConnection.getRequestMethod(),
                        getHttpURLConnection.getURL().toString(),
                        responseCode));
            }

            String statusMessage = "";
            if (responseCode < HttpURLConnection.HTTP_OK || responseCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
                _log.warn(String.format("Response status was: %s. Reason: %s", responseCode,
                        getHttpURLConnection.getResponseMessage()));
                statusMessage = getHttpURLConnection.getResponseMessage();
            }

            inputStreamReader = new InputStreamReader(getHttpURLConnection.getInputStream());
            BufferedReader br = new BufferedReader(inputStreamReader);
            String strCurrentLine;
            String responseBody = new String();
            while ((strCurrentLine = br.readLine()) != null) {
                responseBody = responseBody + strCurrentLine;
            }
            return new SplitHttpResponse(responseCode,
                    statusMessage,
                    responseBody,
                    getResponseHeaders(getHttpURLConnection));
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Problem in http get operation: %s", e), e);
        } finally {
            try {
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (Exception e) {
                _log.error(String.format("Could not close HTTP Stream: %s", e), e);
            }
        }
    }

    public synchronized SplitHttpResponse post(URI uri, HttpEntity entity, Map<String, List<String>> additionalHeaders) throws IOException {
        HttpURLConnection postHttpURLConnection = null;
        try {
            postHttpURLConnection = (HttpURLConnection) uri.toURL().openConnection();
            return _post(postHttpURLConnection, entity, additionalHeaders);
        } catch  (Exception e) {
            throw new IllegalStateException(String.format("Problem in http post operation: %s", e), e);
        } finally {
            try {
                if (postHttpURLConnection != null) {
                    postHttpURLConnection.disconnect();
                }
            } catch (Exception e) {
                _log.error(String.format("Could not close URL Connection: %s", e), e);
            }
        }
    }

    public SplitHttpResponse _post(HttpURLConnection postHttpURLConnection,
                                                HttpEntity entity,
                                                Map<String, List<String>> additionalHeaders)
            throws IOException {
        try {
            postHttpURLConnection.setRequestMethod("POST");
            setBasicHeaders(postHttpURLConnection);
            setAdditionalAndDecoratedHeaders(postHttpURLConnection, additionalHeaders);

            if (postHttpURLConnection.getHeaderField("Accept-Encoding") == null) {
                postHttpURLConnection.setRequestProperty("Accept-Encoding", "gzip");
            }
            postHttpURLConnection.setRequestProperty("Content-Type", "application/json");
            _log.debug(String.format("Request Headers: %s", postHttpURLConnection.getRequestProperties()));

            postHttpURLConnection.setDoOutput(true);
            String postBody = EntityUtils.toString(entity);
            OutputStream os = postHttpURLConnection.getOutputStream();
            os.write(postBody.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
            _log.debug(String.format("Posting: %s", postBody));

            int responseCode = postHttpURLConnection.getResponseCode();
            String statusMessage = "";
            if (responseCode < HttpURLConnection.HTTP_OK || responseCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
                statusMessage = postHttpURLConnection.getResponseMessage();
                _log.warn(String.format("Response status was: %s. Reason: %s", responseCode,
                        statusMessage));
            }
            return new SplitHttpResponse(responseCode, statusMessage, "", getResponseHeaders(postHttpURLConnection));
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Problem in http post operation: %s", e), e);
        }
    }

    private void setBasicHeaders(HttpURLConnection urlConnection) {
        urlConnection.setRequestProperty(HEADER_API_KEY, "Bearer " + _apikey);
        urlConnection.setRequestProperty(HEADER_CLIENT_VERSION, _metadata.getSdkVersion());
        urlConnection.setRequestProperty(HEADER_CLIENT_MACHINE_IP, _metadata.getMachineIp());
        urlConnection.setRequestProperty(HEADER_CLIENT_MACHINE_NAME, _metadata.getMachineName());
        urlConnection.setRequestProperty(HEADER_CLIENT_KEY, _apikey.length() > 4
                ? _apikey.substring(_apikey.length() - 4)
                : _apikey);
    }

    private void setAdditionalAndDecoratedHeaders(HttpURLConnection urlConnection, Map<String, List<String>> additionalHeaders) {
        if (additionalHeaders != null) {
            for (Map.Entry<String, List<String>> entry : additionalHeaders.entrySet()) {
                for (String value : entry.getValue()) {
                    urlConnection.setRequestProperty(entry.getKey(), value);
                }
            }
        }
        HttpRequest request = new HttpGet("");
        _requestDecorator.decorateHeaders(request);
        for (Header header : request.getHeaders()) {
            urlConnection.setRequestProperty(header.getName(), header.getValue());
        }
        request = null;
    }

    private Header[] getResponseHeaders(HttpURLConnection urlConnection) {
        List<BasicHeader> responseHeaders = new ArrayList<BasicHeader>();
        Map<String, List<String>> map = urlConnection.getHeaderFields();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                BasicHeader responseHeader = new BasicHeader(entry.getKey(), entry.getValue());
                responseHeaders.add(responseHeader);
            }
        }
        return responseHeaders.toArray(new Header[0]);

    }
    @Override
    public void close() throws IOException {

    }
}
