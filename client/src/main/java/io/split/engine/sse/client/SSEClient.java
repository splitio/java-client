package io.split.engine.sse.client;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.telemetry.domain.StreamingEvent;
import io.split.telemetry.domain.enums.StreamEventsEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

public class SSEClient {

    public enum StatusMessage {
        CONNECTED,
        RETRYABLE_ERROR,
        NONRETRYABLE_ERROR,
        INITIALIZATION_IN_PROGRESS,
        FORCED_STOP,
        FIRST_EVENT
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
    private AtomicBoolean _forcedStop;

    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    public SSEClient(Function<RawEvent, Void> eventCallback,
                     Function<StatusMessage, Void> statusCallback,
                     CloseableHttpClient client,
                     TelemetryRuntimeProducer telemetryRuntimeProducer) {
        _eventCallback = eventCallback;
        _statusCallback = statusCallback;
        _client = client;
        _forcedStop = new AtomicBoolean();
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
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
            if(e.getMessage() == null){
                _log.info("The thread was interrupted while opening SSEClient");
                return false;
            }
            _log.info(e.getMessage());
            return false;
        }
        return isOpen();
    }

    public boolean isOpen() {
        return (ConnectionState.OPEN.equals(_state.get()));
    }

    public synchronized void close() {
        _forcedStop.set(true);
        if (_state.compareAndSet(ConnectionState.OPEN, ConnectionState.CLOSED)) {
            if (_ongoingResponse.get() != null) {
                try {
                    _ongoingRequest.get().abort();
                    _ongoingResponse.get().close();
                } catch (IOException e) {
                    _log.debug(String.format("SSEClient close forced: %s", e.getMessage()));
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
                        _telemetryRuntimeProducer.recordStreamingEvents(new StreamingEvent(StreamEventsEnum.SSE_CONNECTION_ERROR.getType(), StreamEventsEnum.SseConnectionErrorValues.REQUESTED_CONNECTION_ERROR.getValue(), System.currentTimeMillis()));
                        return;
                    }
                    // Connection closed by server
                    _statusCallback.apply(StatusMessage.RETRYABLE_ERROR);
                    _telemetryRuntimeProducer.recordStreamingEvents(new StreamingEvent(StreamEventsEnum.SSE_CONNECTION_ERROR.getType(), StreamEventsEnum.SseConnectionErrorValues.NON_REQUESTED_CONNECTION_ERROR.getValue(), System.currentTimeMillis()));
                    return;
                } catch (IOException exc) { // Other type of connection error
                    if(!_forcedStop.get()) {
                        _log.debug(String.format("SSE connection ended abruptly: %s. Retying", exc.getMessage()));
                        _telemetryRuntimeProducer.recordStreamingEvents(new StreamingEvent(StreamEventsEnum.SSE_CONNECTION_ERROR.getType(), StreamEventsEnum.SseConnectionErrorValues.REQUESTED_CONNECTION_ERROR.getValue(), System.currentTimeMillis()));
                        _statusCallback.apply(StatusMessage.RETRYABLE_ERROR);
                        return;
                    }

                    _telemetryRuntimeProducer.recordStreamingEvents(new StreamingEvent(StreamEventsEnum.SSE_CONNECTION_ERROR.getType(), StreamEventsEnum.SseConnectionErrorValues.NON_REQUESTED_CONNECTION_ERROR.getValue(), System.currentTimeMillis()));
                }
            }
        } catch (Exception e) { // Any other error non related to the connection disables streaming altogether

            _telemetryRuntimeProducer.recordStreamingEvents(new StreamingEvent(StreamEventsEnum.SSE_CONNECTION_ERROR.getType(), StreamEventsEnum.SseConnectionErrorValues.NON_REQUESTED_CONNECTION_ERROR.getValue(), System.currentTimeMillis()));
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
            _forcedStop.set(false);
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