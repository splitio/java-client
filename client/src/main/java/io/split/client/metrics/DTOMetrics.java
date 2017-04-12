package io.split.client.metrics;

import io.split.client.dtos.Counter;
import io.split.client.dtos.Latency;

/**
 * Created by adilaijaz on 6/14/16.
 */
public interface DTOMetrics {
    void time(Latency dto);

    void count(Counter dto);
}
