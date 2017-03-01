package io.split.identify;

import io.split.identify.dto.Environment;
import io.split.identify.dto.Identity;
import io.split.identify.dto.Property;
import io.split.identify.dto.Type;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class Client {
    abstract List<Type> getTypes();

    abstract List<Environment> getEnvironments();

    abstract List<Property> getProperties(String typeId);

    public List<Property> getProperties(Type type) {
        return getProperties(type.id());
    }

    abstract void createProperty(Property property) throws IllegalArgumentException;

    abstract void deleteProperty(String typeId, String propertyId) throws NoSuchElementException;

    public void deleteProperty(Property property) throws NoSuchElementException {
        deleteProperty(property.typeId(), property.id());
    }

    abstract void saveIdentity(Identity identity);

    abstract void saveIdentities(String typeId, String environmentId, Collection<Identity> identities);

    public void saveIdentities(Collection<Identity> identities) {
        // Group By type & environment, then call
        // saveIdentities(typeId, environmentId, groupedIdentities);
    }

    abstract void updateIdentity(Identity identity);

    abstract void deleteIdentity(String typeId, String environmentId, String key);

    public void deleteIdentity(Identity identity) {
        deleteIdentity(identity.typeId(), identity.environmentId(), identity.key());
    }
}
