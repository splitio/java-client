package io.split.client.impressions.newrelic;

import io.split.client.impressions.Impression;
import io.split.client.impressions.ImpressionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static java.lang.invoke.MethodType.methodType;

/**
 * Impression Listener implementation for New Relic that adds custom parameters to the active transaction
 * in New Relic.
 */
public class NewRelicListener implements ImpressionListener  {
    private static final Logger _log = LoggerFactory.getLogger(NewRelicListener.class);

    //private final Method _addCustomParameter;
    private final MethodHandle _addCustomParameterMethodHandle;

    public NewRelicListener() throws ClassNotFoundException {
        _addCustomParameterMethodHandle = getAddCustomParameterMethodHandle();
        if (_addCustomParameterMethodHandle == null) {
            throw new ClassNotFoundException();
        }
    }

    private MethodHandle getAddCustomParameterMethodHandle() {
        try {
            Class<?> newRelicInstance = Class.forName("com.newrelic.api.agent.NewRelic");
            return MethodHandles.lookup().findStatic(newRelicInstance, "addCustomParameter", methodType(String.class, String.class));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }

    @Override
    public void log(Impression impression) {
        try {
            String entryKey = String.format("split.%s", impression.split());
            _addCustomParameterMethodHandle.invokeExact("split_key", impression.key());
            _addCustomParameterMethodHandle.invokeExact(entryKey, impression.treatment());
        } catch (Throwable e) {
            _log.warn("Unexpected error on New Relic Integration", e);
        }
    }

    @Override
    public void close() {

    }
}
