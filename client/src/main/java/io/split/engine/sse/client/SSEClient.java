package io.split.engine.sse.client;

import com.google.common.base.Strings;
import io.split.engine.sse.EventSourceClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

public class SSEClient {

    public enum StatusMessage {
        CONNECTED,
        RETRYABLE_ERROR,
        NONRETRYABLE_ERROR,
        DISCONNECTED
    }

    private enum ConnectionState {
        OPEN,
        CLOSED
    }

    private final static String KEEP_ALIVE_PAYLOAD = ":keepalive\n";
    private final static Integer CONNECT_TIMEOUT = 30000;
    private final static Integer SOCKET_TIMEOUT = 70000;
    private static final Logger _log = LoggerFactory.getLogger(SSEClient.class);

    private final CloseableHttpClient _client;
    private final Function<RawEvent, Void> _eventCallback;
    private final Function<StatusMessage, Void> _statusCallback;
    private final AtomicReference<ConnectionState> _state = new AtomicReference<>(ConnectionState.CLOSED);
    private final AtomicReference<CloseableHttpResponse> _ongoingResponse = new AtomicReference<>();

    public SSEClient(Function<RawEvent, Void> eventCallback, Function<StatusMessage, Void> statusCallback) {
        _client = buildHttpClient();
        _eventCallback = eventCallback;
        _statusCallback = statusCallback;
    }

    public synchronized boolean open(URI uri) {
        if (isOpen()) {
            _log.warn("SSEClient already open.");
            return false;
        }

        CountDownLatch signal = new CountDownLatch(1);
        Thread thread = new Thread(() -> connectAndLoop(uri, signal));
        thread.setDaemon(true);
        thread.start();
        try {
            signal.await(CONNECT_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            _log.warn(e.getMessage());
            return false;
        }
        return isOpen();
    }

    public boolean isOpen() {
        return (ConnectionState.OPEN.equals(_state.get()));
    }

    public synchronized void close() {
        if (_state.compareAndSet(ConnectionState.OPEN, ConnectionState.CLOSED)) {
            if (_ongoingResponse.get() != null) {
                try {
                    _ongoingResponse.get().close();
                } catch (IOException e) {
                    _log.warn(String.format("Error closing SSEClient: %s", e.getMessage()));
                }
            }
        }
    }

    private void connectAndLoop(URI uri, CountDownLatch signal) {
        checkNotNull(uri);
        checkNotNull(signal);
        if (!establishConnection(uri, signal)) {
            _statusCallback.apply(StatusMessage.NONRETRYABLE_ERROR);
            return;
        }

        try {
            final InputStream stream = _ongoingResponse.get().getEntity().getContent();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            while (isOpen() && !Thread.currentThread().isInterrupted()) {
                try {
                    handleMessage(readMessageAsString(reader));
                } catch (EOFException exc) {
                    // This is when ably closes the connection on their end. IE: an invalid or expired token.
                    // Evaluate if we should send the DISCONNECTED event or not.
                    _statusCallback.apply(StatusMessage.DISCONNECTED);
                    return;
                } catch (SocketTimeoutException exc) { // KeepAlive expired
                    _statusCallback.apply(StatusMessage.RETRYABLE_ERROR);
                } catch (SocketException exc) { // Connection closed by us
                    if ("Socket closed".equals(exc.getMessage())) {
                        _statusCallback.apply(StatusMessage.DISCONNECTED);
                        return;
                    }
                    throw exc; // If it's not a socket closed (caused by us), rethrow the exception
                }
            }
        } catch (IOException e) {
            _log.warn(e.getMessage());
            _statusCallback.apply(StatusMessage.NONRETRYABLE_ERROR);
        } finally {
            try {
                _ongoingResponse.get().close();
            } catch (IOException e) {
                _log.warn(e.getMessage());
            }

            _log.warn("SSEClient finished.");
        }
    }

    private boolean establishConnection(URI uri, CountDownLatch signal) {
        HttpGet request = new HttpGet(uri);

        try {
            _ongoingResponse.set(_client.execute(request));
            if (_ongoingResponse.get().getStatusLine().getStatusCode() != 200) {
                return false;
            }
            _state.set(ConnectionState.OPEN);
            _statusCallback.apply(StatusMessage.CONNECTED);
        } catch (IOException exc) {
            _log.warn(String.format("Error establishConnection: %s", exc.getMessage()));
            exc.printStackTrace();
            return false;
        } finally {
            signal.countDown();
        }
        return true;
    }

    static private String readMessageAsString(BufferedReader reader) throws IOException {
        StringBuilder lines = new StringBuilder();
        while (true) {
            String line = reader.readLine();
            if (null == line) { // EOF (Remote host closed the connection gracefully and the stream ended)
                throw new EOFException("connection closed by remote host");
            }
            if (line.isEmpty()) { // An empty line is a message separator
                return lines.toString();
            }
            lines.append(line).append("\n");
        }
    }

    private static CloseableHttpClient buildHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .build();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(1);
        cm.setDefaultMaxPerRoute(1);

        return HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    private void handleMessage(String message) {
        if (Strings.isNullOrEmpty(message) || KEEP_ALIVE_PAYLOAD.equals(message)) {
            _log.debug("Keep Alive event");
            return;
        }

        RawEvent e = RawEvent.fromString(message);
        _eventCallback.apply(e);
    }
}