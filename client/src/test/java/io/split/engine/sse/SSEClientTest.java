package io.split.engine.sse;

import io.split.client.RequestDecorator;
import io.split.engine.sse.client.RawEvent;
import io.split.engine.sse.client.SSEClient;
import io.split.engine.sse.client.SSEClient.StatusMessage;
import io.split.telemetry.domain.StreamingEvent;
import io.split.telemetry.domain.enums.StreamEventsEnum;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SSEClientTest {

    private static final long WAIT_MILLIS = 5000;
    private static final String SSE_MESSAGE = "id: 1\nevent: message\ndata: {}\n\n";
    private static final URI STUB_URI = URI.create("http://localhost/sse");

    @Test(timeout = 15000)
    public void closeBeforeOpenThenDroppedConnectionReportsRetryableErrorTest() throws Exception {
        TelemetryRuntimeProducer telemetry = Mockito.mock(TelemetryRuntimeProducer.class);
        BlockingQueue<StatusMessage> statuses = new LinkedBlockingQueue<>();
        BlockingQueue<RawEvent> events = new LinkedBlockingQueue<>();
        try (ServerSocket server = new ServerSocket(0);
             CloseableHttpClient httpClient = HttpClients.custom().build()) {
            startServer(server, SSEClientTest::respondThenDrop);
            SSEClient sse = newClient(httpClient, telemetry, statuses, events);
            try {
                sse.close();
                sse.open(uri(server));

                Assert.assertEquals(StatusMessage.INITIALIZATION_IN_PROGRESS, nextStatus(statuses));
                Assert.assertEquals(StatusMessage.CONNECTED, nextStatus(statuses));
                Assert.assertEquals(StatusMessage.RETRYABLE_ERROR, nextStatus(statuses));
                // The loop ends on its own here, so awaitClosed means it has returned and all telemetry is recorded.
                awaitClosed(sse);
                verifyConnectionErrorEvents(telemetry,
                        StreamEventsEnum.SseConnectionErrorValues.NON_REQUESTED_CONNECTION_ERROR);
            } finally {
                sse.close();
            }
        }
    }

    /**
     * The stream blocks in read() after serving one complete message. The test only calls close() once the stream
     * reports the reader is parked in that read(), so the connection loop has already passed its isOpen() check.
     * close() then always takes the abort path (SocketException "Socket closed") and the forced stop is reported.
     */
    @Test(timeout = 15000)
    public void closeMidStreamReportsForcedStopWithRequestedTelemetryTest() throws Exception {
        TelemetryRuntimeProducer telemetry = Mockito.mock(TelemetryRuntimeProducer.class);
        BlockingQueue<StatusMessage> statuses = new LinkedBlockingQueue<>();
        BlockingQueue<RawEvent> events = new LinkedBlockingQueue<>();
        ScriptedStream stream = new ScriptedStream(SSE_MESSAGE, true, true);
        SSEClient sse = newClient(mockClient(streamingResponse(stream)), telemetry, statuses, events);
        try {
            Assert.assertTrue(sse.open(STUB_URI));
            Assert.assertEquals(StatusMessage.INITIALIZATION_IN_PROGRESS, nextStatus(statuses));
            Assert.assertEquals(StatusMessage.CONNECTED, nextStatus(statuses));
            awaitStreamingEvent(events);
            Assert.assertTrue("reader should be blocked in read() before closing", stream.awaitBlocked());

            sse.close();

            Assert.assertEquals(StatusMessage.FORCED_STOP, nextStatus(statuses));
            stream.awaitReaderFinished();
            verifyConnectionErrorEvents(telemetry,
                    StreamEventsEnum.SseConnectionErrorValues.REQUESTED_CONNECTION_ERROR);
        } finally {
            sse.close();
            stream.abort();
        }
    }

    /**
     * Same synchronisation as closeMidStreamReportsForcedStopWithRequestedTelemetryTest: the first connection is
     * only closed once its reader is parked in read(). The second connection ends on the server side and must
     * report a retryable error, not inherit the forced stop.
     */
    @Test(timeout = 15000)
    public void forcedStopIsResetBetweenConnectionsTest() throws Exception {
        TelemetryRuntimeProducer telemetry = Mockito.mock(TelemetryRuntimeProducer.class);
        BlockingQueue<StatusMessage> statuses = new LinkedBlockingQueue<>();
        BlockingQueue<RawEvent> events = new LinkedBlockingQueue<>();
        ScriptedStream first = new ScriptedStream(SSE_MESSAGE, true, true);
        ScriptedStream second = new ScriptedStream("", false, false);
        SSEClient sse = newClient(mockClient(streamingResponse(first), streamingResponse(second)),
                telemetry, statuses, events);
        try {
            // First connection is stopped by the client.
            Assert.assertTrue(sse.open(STUB_URI));
            Assert.assertEquals(StatusMessage.INITIALIZATION_IN_PROGRESS, nextStatus(statuses));
            Assert.assertEquals(StatusMessage.CONNECTED, nextStatus(statuses));
            awaitStreamingEvent(events);
            Assert.assertTrue("reader should be blocked in read() before closing", first.awaitBlocked());
            sse.close();
            Assert.assertEquals(StatusMessage.FORCED_STOP, nextStatus(statuses));
            // Wait for the first connection's second response close; the reopen is safe because SSEClient runs
            // connectAndLoop on a single-thread executor, so the second connection starts after the first loop returns.
            first.awaitReaderFinished();

            // Second connection is ended by the server: must not inherit the forced stop.
            sse.open(STUB_URI);
            Assert.assertEquals(StatusMessage.INITIALIZATION_IN_PROGRESS, nextStatus(statuses));
            Assert.assertEquals(StatusMessage.CONNECTED, nextStatus(statuses));
            Assert.assertEquals(StatusMessage.RETRYABLE_ERROR, nextStatus(statuses));
            awaitClosed(sse);
            verifyConnectionErrorEvents(telemetry,
                    StreamEventsEnum.SseConnectionErrorValues.REQUESTED_CONNECTION_ERROR,
                    StreamEventsEnum.SseConnectionErrorValues.NON_REQUESTED_CONNECTION_ERROR);
        } finally {
            sse.close();
            first.abort();
            second.abort();
        }
    }

    /**
     * A socket closed by close() surfaces as SocketException("Socket closed"), which takes a different branch
     * than a generic IOException. This test stubs the response so the stream fails with a plain IOException
     * once the client is closed, covering the forced-stop IOException branch.
     */
    @Test(timeout = 15000)
    public void closeMidReadWithIOExceptionReportsForcedStopTest() throws Exception {
        TelemetryRuntimeProducer telemetry = Mockito.mock(TelemetryRuntimeProducer.class);
        BlockingQueue<StatusMessage> statuses = new LinkedBlockingQueue<>();
        BlockingQueue<RawEvent> events = new LinkedBlockingQueue<>();
        ScriptedStream stream = new ScriptedStream("", true, false);
        SSEClient sse = newClient(mockClient(streamingResponse(stream)), telemetry, statuses, events);
        try {
            Assert.assertTrue(sse.open(STUB_URI));
            Assert.assertEquals(StatusMessage.INITIALIZATION_IN_PROGRESS, nextStatus(statuses));
            Assert.assertEquals(StatusMessage.CONNECTED, nextStatus(statuses));
            Assert.assertTrue("reader should be blocked in read() before closing", stream.awaitBlocked());

            sse.close();

            Assert.assertEquals(StatusMessage.FORCED_STOP, nextStatus(statuses));
            stream.awaitReaderFinished();
            verifyConnectionErrorEvents(telemetry,
                    StreamEventsEnum.SseConnectionErrorValues.REQUESTED_CONNECTION_ERROR);
            // The forced stop is reported exactly once: nothing follows the loop end.
            Assert.assertNull("no status should follow FORCED_STOP", statuses.poll());
        } finally {
            sse.close();
            stream.abort();
        }
    }

    @Test
    @Ignore
    public void basicUsageTest() throws URISyntaxException, InterruptedException {
        URI uri = new URIBuilder("https://streaming.split.io/sse")
                .addParameter("accessToken", "X")
                .addParameter("v", "1,1")
                .addParameter("channels", "[?occupancy=metrics.publishers]control_pri")
                .build();
        TelemetryRuntimeProducer telemetryRuntimeProducer = Mockito.mock(InMemoryTelemetryStorage.class);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(70000))
                .build();

        HttpClientBuilder httpClientbuilder = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig);

        CloseableHttpClient httpClient =  httpClientbuilder.build();

        SSEClient sse = new SSEClient(e -> null,
                s -> null, httpClient, telemetryRuntimeProducer, null, new RequestDecorator(null));
        sse.open(uri);
        Thread.sleep(5000);
        sse.close();
        Thread.sleep(100000);
    }

    /**
     * Scripted response body. It serves {@code data}, then either ends the stream or blocks in read() until the
     * response is closed. {@link #awaitBlocked()} returns only once the reader is parked in that blocking read,
     * which means it has consumed the data and already passed the connection loop's isOpen() check.
     */
    private static final class ScriptedStream extends InputStream {
        private final byte[] _data;
        private final boolean _holdOpen;
        private final boolean _socketClosedOnAbort;
        private final CountDownLatch _blocked = new CountDownLatch(1);
        private final CountDownLatch _aborted = new CountDownLatch(1);
        // close() closes the response once and the connection loop's finally closes it again.
        private final CountDownLatch _closes = new CountDownLatch(2);
        private int _pos;

        ScriptedStream(String data, boolean holdOpen, boolean socketClosedOnAbort) {
            _data = data.getBytes(StandardCharsets.UTF_8);
            _holdOpen = holdOpen;
            _socketClosedOnAbort = socketClosedOnAbort;
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int n = read(one, 0, 1);
            return n < 0 ? -1 : one[0] & 0xFF;
        }

        @Override
        public int read(byte[] buffer, int off, int len) throws IOException {
            if (_pos < _data.length) {
                int n = Math.min(len, _data.length - _pos);
                System.arraycopy(_data, _pos, buffer, off, n);
                _pos += n;
                return n;
            }
            if (!_holdOpen) {
                return -1;
            }
            _blocked.countDown();
            try {
                _aborted.await(WAIT_MILLIS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (_socketClosedOnAbort) {
                throw new SocketException("Socket closed");
            }
            throw new IOException("Stream aborted");
        }

        boolean awaitBlocked() throws InterruptedException {
            return _blocked.await(WAIT_MILLIS, TimeUnit.MILLISECONDS);
        }

        /**
         * Waits for the second response close, which is the first line of the finally block in
         * SSEClient.connectAndLoop. That runs before _state is set to CLOSED and _forcedStop is reset, so it does not
         * mean the loop has fully returned. Reopening after it is safe only because SSEClient runs connectAndLoop on a
         * single-thread executor, which starts the next connection after the previous loop has returned.
         */
        void awaitReaderFinished() throws InterruptedException {
            Assert.assertTrue("connection loop should finish after the forced stop",
                    _closes.await(WAIT_MILLIS, TimeUnit.MILLISECONDS));
        }

        void onResponseClosed() {
            _aborted.countDown();
            _closes.countDown();
        }

        void abort() {
            _aborted.countDown();
        }
    }

    private static CloseableHttpResponse streamingResponse(ScriptedStream stream) {
        BasicClassicHttpResponse classicResponse = new BasicClassicHttpResponse(200);
        classicResponse.setEntity(new InputStreamEntity(stream, -1, ContentType.TEXT_PLAIN));
        return CloseableHttpResponse.create(classicResponse, (closeable, closeMode) -> stream.onResponseClosed());
    }

    private static CloseableHttpClient mockClient(CloseableHttpResponse first, CloseableHttpResponse... next)
            throws IOException {
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Mockito.when(httpClient.execute(Mockito.any(ClassicHttpRequest.class))).thenReturn(first, next);
        return httpClient;
    }

    private static SSEClient newClient(CloseableHttpClient httpClient, TelemetryRuntimeProducer telemetry,
                                       BlockingQueue<StatusMessage> statuses, BlockingQueue<RawEvent> events) {
        return new SSEClient(e -> {
            events.add(e);
            return null;
        }, s -> {
            statuses.add(s);
            return null;
        }, httpClient, telemetry, null, new RequestDecorator(null));
    }

    private static URI uri(ServerSocket server) {
        return URI.create("http://localhost:" + server.getLocalPort() + "/sse");
    }

    private static StatusMessage nextStatus(BlockingQueue<StatusMessage> statuses) throws InterruptedException {
        StatusMessage status = statuses.poll(WAIT_MILLIS, TimeUnit.MILLISECONDS);
        Assert.assertNotNull("expected a status callback within " + WAIT_MILLIS + " ms", status);
        return status;
    }

    /** Waits until the reader has delivered the server message to the event callback. */
    private static void awaitStreamingEvent(BlockingQueue<RawEvent> events) throws InterruptedException {
        Assert.assertNotNull("expected the server event within " + WAIT_MILLIS + " ms",
                events.poll(WAIT_MILLIS, TimeUnit.MILLISECONDS));
    }

    /**
     * Exact check that the expected connection-error telemetry was recorded and nothing else was. Call it only after
     * the loop has finished recording telemetry (awaitClosed for loops that end on their own,
     * ScriptedStream.awaitReaderFinished when the test closed the client; the latter fires at the start of the
     * finally block, after the last recordStreamingEvents call). A duplicate or late call then fails here instead
     * of being missed by a timed wait.
     */
    private static void verifyConnectionErrorEvents(TelemetryRuntimeProducer telemetry,
                                                    StreamEventsEnum.SseConnectionErrorValues... expected) {
        ArgumentCaptor<StreamingEvent> captor = ArgumentCaptor.forClass(StreamingEvent.class);
        Mockito.verify(telemetry, Mockito.times(expected.length)).recordStreamingEvents(captor.capture());
        Mockito.verifyNoMoreInteractions(telemetry);
        List<StreamingEvent> recorded = captor.getAllValues();
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(StreamEventsEnum.SSE_CONNECTION_ERROR.getType(), recorded.get(i).getType());
            Assert.assertEquals(expected[i].getValue(), recorded.get(i).getData());
        }
    }

    /**
     * Waits for isOpen() to become false. Only reliable when the connection loop ends by itself, since close()
     * sets the state to closed before the loop has actually returned.
     */
    private static void awaitClosed(SSEClient sse) throws InterruptedException {
        long deadline = System.currentTimeMillis() + WAIT_MILLIS;
        while (sse.isOpen() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        Assert.assertFalse("SSE client should be closed once the connection loop ends", sse.isOpen());
    }

    private interface SocketBehavior {
        void run(Socket socket) throws Exception;
    }

    private static void startServer(ServerSocket server, SocketBehavior... behaviors) {
        Thread serverThread = new Thread(() -> {
            for (SocketBehavior behavior : behaviors) {
                try (Socket socket = server.accept()) {
                    behavior.run(socket);
                } catch (Exception ignored) {
                }
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private static void respondThenDrop(Socket socket) throws IOException {
        readRequestHead(socket);
        writeStreamHeaders(socket);
    }

    private static void readRequestHead(Socket socket) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
        String line;
        do {
            line = reader.readLine();
        } while (line != null && !line.isEmpty());
    }

    private static void writeStreamHeaders(Socket socket) throws IOException {
        OutputStream out = socket.getOutputStream();
        out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\n"
                + "Connection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
