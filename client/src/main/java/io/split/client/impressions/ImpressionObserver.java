package io.split.client.impressions;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.split.client.dtos.KeyImpression;
import org.apache.http.annotation.NotThreadSafe;

/*
According to guava's docs (https://guava.dev/releases/18.0/api/docs/com/google/common/annotations/Beta.html),
the @Beta decorator only means that the api is not frozen, and has nothing to do with behaviour stability, but
rather to a non-frozen API which may introduce breaking changes at any time in future versions.
Since the library is shaded and should not be exposed to users of the SDK, it's safe to use it here.
 */

@SuppressWarnings("UnstableApiUsage")
@NotThreadSafe
public class ImpressionObserver {

    private final Cache<Long, Long> _cache;

    public ImpressionObserver(long size) {
        _cache = CacheBuilder.newBuilder()
                .maximumSize(size)
                .concurrencyLevel(4)  // Just setting the default value explicitly
                .build();
    }
    
    public Long testAndSet(KeyImpression impression) {
        if (null == impression) {
            return null;
        }

        Long hash = ImpressionHasher.process(impression);
        Long previous = _cache.getIfPresent(hash);
        _cache.put(hash, impression.time);
        return previous;
    }
}