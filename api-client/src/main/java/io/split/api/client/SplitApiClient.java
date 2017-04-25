package io.split.api.client;

public interface SplitApiClient {
    TrafficTypeClient trafficTypes();

    EnvironmentClient environments();

    AttributeClient attributes();

    IdentityClient identities();
}
