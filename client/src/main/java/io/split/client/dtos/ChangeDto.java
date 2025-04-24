package io.split.client.dtos;

import java.util.ArrayList;
import java.util.List;

public class ChangeDto<T> {
    public long s;
    public long t;
    public List<T> d;

    public static ChangeDto createEmptyDto() {
        ChangeDto dto = new ChangeDto<>();
        dto.d = new ArrayList<>();
        dto.t = -1;
        dto.s = -1;
        return dto;
    }
}