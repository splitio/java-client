package io.split.storages.pluggable.adapters;

import io.split.client.dtos.Event;
import io.split.client.events.EventsStorageProducer;
import io.split.storages.pluggable.CustomStorageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCustomEventAdapterProducer implements EventsStorageProducer {

    private static final Logger _log = LoggerFactory.getLogger(UserCustomEventAdapterProducer.class);

    private final CustomStorageWrapper _customStorageWrapper;

    public UserCustomEventAdapterProducer(CustomStorageWrapper customStorageWrapper) {
        _customStorageWrapper = checkNotNull(customStorageWrapper);
    }

    @Override
    public boolean track(Event event, int eventSize) {
        return false;
    }
}
