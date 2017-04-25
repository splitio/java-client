package io.split.api.client.api;

import io.split.api.client.IdentityClient;
import io.split.api.dtos.Identity;

import java.util.Collection;
import java.util.NoSuchElementException;

public class ApiIdentityClient implements IdentityClient {

    public ApiIdentityClient save(Identity identity) {
        return this;
    }

    public ApiIdentityClient save(String trafficTypeId, String environmentId, Collection<Identity> identities) {
        return this;
    }

    public ApiIdentityClient save(Collection<Identity> identities) {
        // Group By trafficType & environment, then call
        // saveIdentities(trafficTypeId, environmentId, groupedIdentities);
        return this;
    }

    public ApiIdentityClient update(Identity identity) throws NoSuchElementException {
        return this;
    }

    public ApiIdentityClient delete(String trafficTypeId, String environmentId, String key) throws NoSuchElementException {
        return this;
    }

    public ApiIdentityClient delete(Identity identity) throws NoSuchElementException {
        delete(identity.trafficTypeId(), identity.environmentId(), identity.key());
        return this;
    }
}
