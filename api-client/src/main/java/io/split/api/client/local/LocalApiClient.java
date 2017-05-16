package io.split.api.client.local;

import io.split.api.client.AttributeClient;
import io.split.api.client.EnvironmentClient;
import io.split.api.client.IdentityClient;
import io.split.api.client.SplitApiClient;
import io.split.api.client.TrafficTypeClient;

/**
 * This implementation is useful for using Split in localhost environment.
 */
public final class LocalApiClient implements SplitApiClient {
    @Override
    public TrafficTypeClient trafficTypes() {
        return new LocalTrafficTypeClient();
    }

    @Override
    public EnvironmentClient environments() {
        return new LocalEnvironmentClient();
    }

    @Override
    public AttributeClient attributes() {
        return new LocalAttributeClient();
    }

    @Override
    public IdentityClient identities() {
        return new LocalIdentityClient();
    }
}
