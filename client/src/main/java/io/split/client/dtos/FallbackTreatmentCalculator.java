package io.split.client.dtos;

public interface FallbackTreatmentCalculator
{
    FallbackTreatment resolve(String flagName, String label);
}
