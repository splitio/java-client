package io.split.api.client;

import io.split.api.dtos.Identity;

import java.util.Collection;
import java.util.NoSuchElementException;

public interface IdentityClient {

    IdentityClient save(Identity identity);

    IdentityClient save(String trafficTypeId, String environmentId, Collection<Identity> identities);

    IdentityClient save(Collection<Identity> identities);

    IdentityClient update(Identity identity) throws NoSuchElementException;

    IdentityClient delete(String trafficTypeId, String environmentId, String key) throws NoSuchElementException;

    IdentityClient delete(Identity identity) throws NoSuchElementException;
}
