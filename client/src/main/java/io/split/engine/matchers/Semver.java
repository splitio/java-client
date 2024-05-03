package io.split.engine.matchers;

import io.split.client.exceptions.SemverParseException;
import io.split.engine.experiments.SplitParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.primitives.Ints;

import java.util.Arrays;

public class Semver {
    private final String MetadataDelimiter = "+";
    private final String PreReleaseDelimiter = "-";
    private final String ValueDelimiter = "\\.";

    private static final Logger _log = LoggerFactory.getLogger(SplitParser.class);

    private Long _major;
    private Long _minor;
    private Long _patch;
    private String[] _preRelease = new String[] {};
    private boolean _isStable;
    private String _metadata;
    private String _version;

    public static Semver build(String version) {
        if (version.isEmpty()) return null;
        try {
            return new Semver(version);
        } catch (Exception ex) {
            _log.error("An error occurred during the creation of a Semver instance:", ex.getMessage());
            return null;
        }
    }

    public String Version() {
      return _version;
    }

    public Long Major() {
        return _major;
    }

    public Long Minor() {
        return _minor;
    }

    public Long Patch() {
        return _patch;
    }

    public String[] PreRelease() {
        return _preRelease;
    }

    public String Metadata() {
        return _metadata;
    }

    public boolean IsStable() {
        return _isStable;
    }

    /**
     * Precedence comparision between 2 Semver objects.
     *
     * @return the value {@code 0} if {@code this == toCompare};
     *         a value less than {@code 0} if {@code this < toCompare}; and
     *         a value greater than {@code 0} if {@code this > toCompare}
     */
    public int Compare(Semver toCompare) {
        if (_version.equals(toCompare.Version())) {
            return 0;
        }
        // Compare major, minor, and patch versions numerically
        int result = Long.compare(_major, toCompare.Major());
        if (result != 0) {
            return result;
        }
        result = Long.compare(_minor, toCompare.Minor());
        if (result != 0) {
            return result;
        }
        result = Long.compare(_patch, toCompare.Patch());
        if (result != 0) {
            return result;
        }
        if (!_isStable && toCompare.IsStable()) {
            return -1;
        } else if (_isStable && !toCompare.IsStable()) {
            return 1;
        }
        // Compare pre-release versions lexically
        int minLength = Math.min(_preRelease.length, toCompare.PreRelease().length);
        for (int i = 0; i < minLength; i++) {
            if (_preRelease[i].equals(toCompare.PreRelease()[i])) {
                continue;
            }
            if ( Ints.tryParse(_preRelease[i]) != null &&  Ints.tryParse(toCompare._preRelease[i]) != null) {
                return Integer.compare(Integer.parseInt(_preRelease[i]), Integer.parseInt(toCompare._preRelease[i]));
            }
            return AdjustNumber(_preRelease[i].compareTo(toCompare._preRelease[i]));
        }
        // Compare lengths of pre-release versions
        return Integer.compare(_preRelease.length, toCompare._preRelease.length);
    }

    private int AdjustNumber(int number) {
        if (number > 0) return 1;
        if (number < 0) return -1;
        return 0;
    }
    private Semver(String version) throws SemverParseException {
        String vWithoutMetadata = setAndRemoveMetadataIfExists(version);
        String vWithoutPreRelease = setAndRemovePreReleaseIfExists(vWithoutMetadata);
        setMajorMinorAndPatch(vWithoutPreRelease);
        _version = setVersion();
    }
    private String setAndRemoveMetadataIfExists(String version) throws SemverParseException {
        int index = version.indexOf(MetadataDelimiter);
        if (index == -1) {
            return version;
        }
        _metadata = version.substring(index+1);
        if (_metadata == null || _metadata.isEmpty()) {
            throw new SemverParseException("Unable to convert to Semver, incorrect pre release data");
        }
        return version.substring(0, index);
    }
    private String setAndRemovePreReleaseIfExists(String vWithoutMetadata) throws SemverParseException {
        int index = vWithoutMetadata.indexOf(PreReleaseDelimiter);
        if (index == -1) {
            _isStable = true;
            return vWithoutMetadata;
        }
        String preReleaseData = vWithoutMetadata.substring(index+1);
        _preRelease = preReleaseData.split(ValueDelimiter);
        if (_preRelease == null || Arrays.stream(_preRelease).allMatch(pr -> pr == null || pr.isEmpty())) {
            throw new SemverParseException("Unable to convert to Semver, incorrect pre release data");
        }
        return vWithoutMetadata.substring(0, index);
    }
    private void setMajorMinorAndPatch(String version) throws SemverParseException {
        String[] vParts = version.split(ValueDelimiter);
        if (vParts.length != 3)
            throw new SemverParseException("Unable to convert to Semver, incorrect format: " + version);
        _major = Long.parseLong(vParts[0]);
        _minor = Long.parseLong(vParts[1]);
        _patch = Long.parseLong(vParts[2]);
    }

    private String setVersion() {
        String toReturn = _major + ValueDelimiter + _minor + ValueDelimiter + _patch;
        if (_preRelease != null && _preRelease.length != 0)
        {
            for (int i = 0; i < _preRelease.length; i++)
            {
                if (isNumeric(_preRelease[i]))
                {
                    _preRelease[i] = Long.toString(Long.parseLong(_preRelease[i]));
                }
            }
            toReturn = toReturn + PreReleaseDelimiter + String.join(ValueDelimiter, _preRelease);
        }
        if (_metadata != null && !_metadata.isEmpty()) {
            toReturn = toReturn + MetadataDelimiter + _metadata;
        }
        return toReturn;
    }

    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }
}
