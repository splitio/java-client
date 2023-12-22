package io.split.integrations;

import io.split.client.impressions.ImpressionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class IntegrationsConfig {

    private static final Logger _log = LoggerFactory.getLogger(IntegrationsConfig.class);

    private List<ImpressionListenerWithMeta> _impressionListeners;

    private IntegrationsConfig(List<ImpressionListenerWithMeta> impressionListeners) {
        _impressionListeners = impressionListeners;
    }

    public List<ImpressionListenerWithMeta> getImpressionsListeners(Execution execution) {
        List<ImpressionListenerWithMeta> filtered = new ArrayList<>();
        for (ImpressionListenerWithMeta listener: _impressionListeners) {
            if (listener.execution().equals(execution)) {
                filtered.add(listener);
            }
        }
        return filtered;
    }

    // This method is used to avoid introducing breaking changes, since the impressions listener
    // is a root-level config option (that method will be deprecated soon).
    public void addStandardImpressionListener(ImpressionListener listener, int queueSize) {
        if (queueSize <= 0) {
            throw new IllegalArgumentException("An ImpressionListener was provided, but its capacity was non-positive: " + queueSize);
        }
        _impressionListeners.add(new ImpressionListenerWithMeta(listener, Execution.ASYNC, queueSize));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private List<ImpressionListenerWithMeta> _listeners;
        private boolean _newRelicEnabled;

        public Builder() {
            _newRelicEnabled = false;
            _listeners = new ArrayList<>();
        }

        public Builder impressionsListener(ImpressionListener listener, int queueSize) {
            if (queueSize <= 0) {
                throw new IllegalArgumentException("An ImpressionListener was provided, but its capacity was non-positive: " + queueSize);
            }
            _listeners.add(new ImpressionListenerWithMeta(listener, Execution.ASYNC, queueSize));
            return this;
        }

        public Builder impressionsListener(ImpressionListener listener, int queueSize, Execution executionType) {
            if (queueSize <= 0) {
                throw new IllegalArgumentException("An ImpressionListener was provided, but its capacity was non-positive: " + queueSize);
            }
            _listeners.add(new ImpressionListenerWithMeta(listener, executionType, queueSize));
            return this;
        }

        public Builder newRelicImpressionListener() {
            if (_newRelicEnabled) {
                _log.warn("You can only add one new relic integration instance. Ignoring");
                return this;
            }

            try {
                _listeners.add(new ImpressionListenerWithMeta(new NewRelicListener(), Execution.SYNC, 0));
                _log.info("Added New Relic Impression Listener");
                _newRelicEnabled = true;
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
                _log.warn("New Relic agent not found. Continuing without it", e);
            } catch (Exception e) {
                _log.warn("Failed to check if the New Relic Agent is running", e);
            }
            return this;
        }

        public IntegrationsConfig build() {
            return new IntegrationsConfig(_listeners);
        }
    }

    public enum Execution {
        SYNC,
        ASYNC
    }

    public static class ImpressionListenerWithMeta {
        private final ImpressionListener _listener;
        private final Execution _execution;
        private final int _queueSize;

        ImpressionListenerWithMeta(ImpressionListener listener, Execution execution, int queueSize) {
            _listener = listener;
            _execution = execution;
            _queueSize = queueSize;
        }

        public ImpressionListener listener() {
            return _listener;
        }

        Execution execution() {
            return _execution;
        }

        public int queueSize() {
            return _queueSize;
        }
    }
}
