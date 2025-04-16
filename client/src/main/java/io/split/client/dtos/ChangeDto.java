package io.split.client.dtos;

import java.util.List;

public class ChangeDto<T> {
    public long s;
    public long t;
    public List<T> d;
}