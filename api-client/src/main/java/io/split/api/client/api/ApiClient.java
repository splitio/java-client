package io.split.api.client.api;

import io.split.api.client.AttributeClient;
import io.split.api.client.EnvironmentClient;
import io.split.api.client.IdentityClient;
import io.split.api.client.SplitApiClient;
import io.split.api.client.TrafficTypeClient;

public class ApiClient implements SplitApiClient {

    public TrafficTypeClient trafficTypes() {
        return new ApiTrafficTypeClient();
    }

    public EnvironmentClient environments() {
        return new ApiEnvironmentClient();
    }

    public AttributeClient attributes() {
        return new ApiAttributeClient();
    }

    public IdentityClient identities() {
        return new ApiIdentityClient();
    }
}
