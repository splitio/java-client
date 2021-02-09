package io.split.engine.sse.client;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

public class SSEClient {

    public enum StatusMessage {
        CONNECTED,
        RETRYABLE_ERROR,
        NONRETRYABLE_ERROR,
        INITIALIZATION_IN_PROGRESS,
        FORCED_STOP
    }

    private enum ConnectionState {
        OPEN,
        CLOSED
    }

    private final static String SOCKET_CLOSED_MESSAGE = "Socket closed";
    private final static String KEEP_ALIVE_PAYLOAD = ":keepalive\n";
    private final static long CONNECT_TIMEOUT = 30000;
    private static final Logger _log = LoggerFactory.getLogger(SSEClient.class);

    private final ExecutorService _connectionExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("SPLIT-SSEConnection-%d")
            .build());
    private final CloseableHttpClient _client;
    private final Function<RawEvent, Void> _eventCallback;
    private final Function<StatusMessage, Void> _statusCallback;
    private final AtomicReference<ConnectionState> _state = new AtomicReference<>(ConnectionState.CLOSED);
    private final AtomicReference<CloseableHttpResponse> _ongoingResponse = new AtomicReference<>();
    private final AtomicReference<HttpGet> _ongoingRequest = new AtomicReference<>();

    public SSEClient(Function<RawEvent, Void> eventCallback,
                     Function<StatusMessage, Void> statusCallback,
                     CloseableHttpClient client) {
        _eventCallback = eventCallback;
        _statusCallback = statusCallback;
        _client = client;
    }

    public synchronized boolean open(URI uri) {
        if (isOpen()) {
            _log.info("SSEClient already open.");
            return false;
        }

        _statusCallback.apply(StatusMessage.INITIALIZATION_IN_PROGRESS);

        CountDownLatch signal = new CountDownLatch(1);
        _connectionExecutor.submit(() -> connectAndLoop(uri, signal));
        try {
            if (!signal.await(CONNECT_TIMEOUT, TimeUnit.SECONDS)) {
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            _log.info(e.getMessage());
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
                    _ongoingRequest.get().abort();
                    _ongoingResponse.get().close();
                } catch (IOException e) {
                    _log.info(String.format("Error closing SSEClient: %s", e.getMessage()));
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
                } catch (SocketException exc) {
                    _log.debug(exc.getMessage());
                    if (SOCKET_CLOSED_MESSAGE.equals(exc.getMessage())) { // Connection closed by us
                        _statusCallback.apply(StatusMessage.FORCED_STOP);
                        return;
                    }
                    // Connection closed by server
                    _statusCallback.apply(StatusMessage.RETRYABLE_ERROR);
                    return;
                } catch (IOException exc) { // Other type of connection error
                    _log.info(String.format("SSE connection ended abruptly: %s. Retrying", exc.getMessage()));
                    _statusCallback.apply(StatusMessage.RETRYABLE_ERROR);
                    return;
                }
            }
        } catch (Exception e) { // Any other error non related to the connection disables streaming altogether
            _log.warn(e.getMessage(), e);
            _statusCallback.apply(StatusMessage.NONRETRYABLE_ERROR);
        } finally {
            try {
                _ongoingResponse.get().close();
            } catch (IOException e) {
                _log.debug(e.getMessage());
            }

            _state.set(ConnectionState.CLOSED);
            _log.debug("SSEClient finished.");
        }
    }

    private boolean establishConnection(URI uri, CountDownLatch signal) {

        _ongoingRequest.set(new HttpGet(uri));

        try {
            _ongoingResponse.set(_client.execute(_ongoingRequest.get()));
            if (_ongoingResponse.get().getCode() != 200) {
                return false;
            }
            _state.set(ConnectionState.OPEN);
            _statusCallback.apply(StatusMessage.CONNECTED);
        } catch (IOException exc) {
            _log.error(String.format("Error establishConnection: %s", exc));
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

    private void handleMessage(String message) {
        if (Strings.isNullOrEmpty(message) || KEEP_ALIVE_PAYLOAD.equals(message)) {
            _log.debug("Keep Alive event");
            return;
        }

        RawEvent e = RawEvent.fromString(message);
        _eventCallback.apply(e);
    }
}