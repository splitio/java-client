package io.split;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.utils.Pair;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class SSEMockServer {
    private static final String BASE_URL = "http://localhost:%d";
    private final SseEventQueue _queue;
    private final Validator _validator;
    private final AtomicInteger _port;
    private HttpServer _server;

    public static final OutboundEvent CONNECTION_CLOSED_BY_REMOTE_HOST = new OutboundEvent.Builder().comment("CCBRH").build();
//    public static final OutboundEvent CONNECTION_RESET_EVENT = new OutboundEvent.Builder().comment("RST").build();

    public SSEMockServer(SseEventQueue queue, Validator validator) {
        _queue = queue;
        _validator = validator;
        _port = new AtomicInteger();
    }

    public synchronized void start() throws IOException {
        if (null != _server) {
            throw new IllegalStateException("server is already running");
        }

        _port.set(getFreePort());
        _server = GrizzlyHttpServerFactory.createHttpServer(URI.create(String.format(BASE_URL, _port.get())),
                new ResourceConfig()
                        .register(SseResource.class)
                        .register(new AbstractBinder() {
                            @Override
                            protected void configure() {
                                bind(_queue).to(SseEventQueue.class);
                                bind(_validator).to(Validator.class);
                            }
                        }));
    }

    public synchronized void stop() {
        if (null == _server) {
            throw new IllegalStateException("Server is not running");
        }

        _server.shutdownNow();
    }

    public int getPort() { return _port.get(); }

    private int getFreePort() throws IOException {
        try (ServerSocket ss = new ServerSocket(0)) {
            return ss.getLocalPort();
        }
    }

    @Singleton
    @Path("")
    public static class SseResource {
        private final SseEventQueue _eventsToSend;
        private final Validator _validator;

        @Inject
        public SseResource(SseEventQueue queue, Validator validator) {
            _eventsToSend = queue;
            _validator = validator;
        }

        @GET
        @Path("/ping")
        public Response ping() {
            return Response.ok().entity("Service online").build();
        }

        @GET
        @Produces("text/event-stream")
        public void getServerSentEvents(@Context SseEventSink eventSink,
                                        @Context Sse sse,
                                        @QueryParam("channels") String channels,
                                        @QueryParam("v") String version,
                                        @QueryParam("accessToken") String token) {
            new Thread(() -> {
                Pair<OutboundSseEvent, Boolean> validationResult = _validator.validate(token, version, channels);
                if (validationResult.getFirst() != null) { // if we need to send an event
                    eventSink.send(validationResult.getFirst());
                }

                if (!validationResult.getSecond()) { // if validation failed and request should be aborted
                    eventSink.close();
                    return;
                }

                while(!eventSink.isClosed()) {
                    try {
                        OutboundSseEvent event = _eventsToSend.pull();
                        if (CONNECTION_CLOSED_BY_REMOTE_HOST == event) { // Comparing references, no need for .equals()
                            eventSink.close();
                            return;
                        }
                        eventSink.send(event);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }).start();
        }
    }

    @Singleton
    @Service
    public static class SseEventQueue {
        private final LinkedBlockingQueue<OutboundSseEvent> _queuedEvents;

        public SseEventQueue() { _queuedEvents = new LinkedBlockingQueue<>(); }
        public void push(OutboundSseEvent e) { _queuedEvents.offer(e); }
        OutboundSseEvent pull() throws InterruptedException { return _queuedEvents.take(); }
    }

    public interface Validator {
        Pair<OutboundSseEvent, Boolean> validate(String token, String version, String channel);
    }
}
