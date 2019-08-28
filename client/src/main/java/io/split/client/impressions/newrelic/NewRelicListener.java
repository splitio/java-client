package io.split.client.impressions.newrelic;

import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Impression Listener implementation for New Relic that adds custom parameters to the active transaction
 * in New Relic.
 */
public class NewRelicListener implements ImpressionListener  {
    private static final Logger _log = LoggerFactory.getLogger(NewRelicListener.class);

    private final Method _addCustomParameter;

    public NewRelicListener() throws ClassNotFoundException, NoSuchMethodException {
        Class<?> newRelicInstance = Class.forName("com.newrelic.api.agent.NewRelic");
        this._addCustomParameter = newRelicInstance.getDeclaredMethod("addCustomParameter", String.class, String.class);
    }

    @Override
    public void log(Impression impression) {
        try {
            String entryKey = String.format("split.%s", impression.split());
            _addCustomParameter.invoke(null, "split_key", impression.key());
            _addCustomParameter.invoke(null, entryKey, impression.treatment());
        } catch (Exception e) {
            _log.warn("Unexpected error on New Relic Integration", e);
        }
    }

    @Override
    public void close() {

    }
}
