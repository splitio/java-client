package io.split.client;

/**
 * Created by adilaijaz on 5/8/15.
 */
public interface SplitFactory {
    SplitClient client();
    SplitManager manager();
    void destroy();
    boolean isDestroyed();
}
