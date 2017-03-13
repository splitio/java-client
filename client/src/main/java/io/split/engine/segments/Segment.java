package io.split.engine.segments;

/**
 * Fetches the keys in a segment. Implementing classes are responsible for keeping
 * the segment up-to-date with the remote server.
 *
 * @author adil
 */
public interface Segment {
    String segmentName();

    /**
     * This method MUST NOT throw any exceptions.
     *
     * @return true if this segment contains the key. false otherwise.
     */
    boolean contains(String key);

    /**
     * Forces a sync of the segment with the remote server, outside of any scheduled
     * syncs. This method MUST NOT throw any exceptions.
     */
    void forceRefresh();
}
