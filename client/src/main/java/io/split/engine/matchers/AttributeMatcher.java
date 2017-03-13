package io.split.engine.matchers;

import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by adilaijaz on 3/4/16.
 */

public final class AttributeMatcher {

    private final String _attribute;
    private final Matcher _matcher;


    public static AttributeMatcher vanilla(Matcher matcher) {
        return new AttributeMatcher(null, matcher, false);
    }

    public AttributeMatcher(String attribute, Matcher matcher, boolean negate) {
        _attribute = attribute;
        checkNotNull(matcher);
        _matcher = new NegatableMatcher(matcher, negate);
    }

    public boolean match(String key, Map<String, Object> attributes) {
        if (_attribute == null) {
            return _matcher.match(key);
        }

        if (attributes == null) {
            return false;
        }

        Object value = attributes.get(_attribute);
        if (value == null) {
            return false;
        }


        return _matcher.match(value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_attribute, _matcher);
    }

    public String attribute() {
        return _attribute;
    }

    public Matcher matcher() {
        return _matcher;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof AttributeMatcher)) return false;

        AttributeMatcher other = (AttributeMatcher) obj;

        return Objects.equals(_attribute, other._attribute)
                && _matcher.equals(other._matcher);
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("key");
        if (_attribute != null) {
            bldr.append(".");
            bldr.append(_attribute);
        }

        bldr.append(" is");
        bldr.append(_matcher);
        return bldr.toString();
    }

    public static final class NegatableMatcher implements Matcher {
        private final boolean _negate;
        private final Matcher _delegate;

        public NegatableMatcher(Matcher matcher, boolean negate) {
            _negate = negate;
            _delegate = matcher;
        }


        @Override
        public boolean match(Object key) {
            boolean result = _delegate.match(key);
            return (_negate) ? !result : result;
        }

        @Override
        public int hashCode() {
            return Objects.hash(_negate, _delegate);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (this == obj) return true;
            if (!(obj instanceof NegatableMatcher)) return false;

            NegatableMatcher other = (NegatableMatcher) obj;

            return _negate == other._negate
                    && _delegate.equals(other._delegate);
        }

        @Override
        public String toString() {
            StringBuilder bldr = new StringBuilder();
            if (_negate) {
                bldr.append(" not");
            }
            bldr.append(" ");
            bldr.append(_delegate);
            return bldr.toString();
        }

        public Matcher delegate() {
            return _delegate;
        }
    }

}
