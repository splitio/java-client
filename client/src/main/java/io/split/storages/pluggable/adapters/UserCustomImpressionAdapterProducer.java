package io.split.storages.pluggable.adapters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import io.split.client.dtos.KeyImpression;
import io.split.client.dtos.Metadata;
import io.split.client.impressions.ImpressionsStorageProducer;
import io.split.storages.pluggable.domain.ImpressionConsumer;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.UserStorageWrapper;
import pluggable.CustomStorageWrapper;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCustomImpressionAdapterProducer implements ImpressionsStorageProducer {

    private final UserStorageWrapper _userStorageWrapper;
    private final Gson _json = new GsonBuilder()
            .serializeNulls()  // Send nulls
            .excludeFieldsWithModifiers(Modifier.STATIC)
            .registerTypeAdapter(Double.class, (JsonSerializer<Double>) (src, typeOfSrc, context) -> {
                if (src == src.longValue())
                    return new JsonPrimitive(src.longValue());
                return new JsonPrimitive(src);
            })
            .create();
    private Metadata _metadata;

    public UserCustomImpressionAdapterProducer(CustomStorageWrapper customStorageWrapper, Metadata metadata) {
        _userStorageWrapper = new UserStorageWrapper(checkNotNull(customStorageWrapper));
        _metadata = metadata;
    }

    @Override
    public long put(List<KeyImpression> imps) {
        //Impression
        if (imps.isEmpty()){
            return 0;
        }
        List<String> impressions = imps.stream().map(keyImp -> _json.toJson(new ImpressionConsumer(_metadata, keyImp))).collect(Collectors.toList());
        return _userStorageWrapper.pushItems(PrefixAdapter.buildImpressions(), impressions);
    }
}
