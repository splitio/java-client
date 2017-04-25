package io.split.api.client.local;

import io.split.api.client.IdentityClient;
import io.split.api.dtos.Identity;

import java.util.Collection;
import java.util.NoSuchElementException;

public class LocalIdentityClient implements IdentityClient {
    @Override
    public IdentityClient save(Identity identity) {
        return this;
    }

    @Override
    public IdentityClient save(String trafficTypeId, String environmentId, Collection<Identity> identities) {
        return this;
    }

    @Override
    public IdentityClient save(Collection<Identity> identities) {
        return this;
    }

    @Override
    public IdentityClient update(Identity identity) throws NoSuchElementException {
        return this;
    }

    @Override
    public IdentityClient delete(String trafficTypeId, String environmentId, String key) throws NoSuchElementException {
        return this;
    }

    @Override
    public IdentityClient delete(Identity identity) throws NoSuchElementException {
        return this;
    }
}
