package io.split.client;

import java.util.*;

public interface UserCustomHeaderDecorator
{
    Map<String, String> getHeaderOverrides();
}
