package io.split.rules.logging;

public interface Logger {
    void debug(String message);
    void warn(String message);
    void error(String message);
    void error(String message, Throwable t);
}
