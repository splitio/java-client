package io.split.storages.pluggable.adapters;

import io.split.client.dtos.Event;
import io.split.client.dtos.Metadata;
import io.split.client.events.EventsStorageProducer;
import io.split.client.utils.Json;
import io.split.storages.pluggable.CustomStorageWrapper;
import io.split.storages.pluggable.domain.EventConsumer;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.SafeUserStorageWrapper;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCustomEventAdapterProducer implements EventsStorageProducer {

    private final SafeUserStorageWrapper _safeUserStorageWrapper;
    private Metadata _metadata;

    public UserCustomEventAdapterProducer(CustomStorageWrapper customStorageWrapper, Metadata metadata) {
        _safeUserStorageWrapper = new SafeUserStorageWrapper(checkNotNull(customStorageWrapper));
        _metadata = metadata;
    }

    @Override
    public boolean track(Event event, int eventSize) {
        List<String> events = Stream.of(Json.toJson(new EventConsumer(_metadata, event))).collect(Collectors.toList());
        _safeUserStorageWrapper.pushItems(PrefixAdapter.buildEvent(), events);
        return true;
    }
}
