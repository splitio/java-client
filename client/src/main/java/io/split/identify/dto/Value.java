package io.split.identify.dto;

import java.util.Date;

class Value {
    private String value;
    private String type;

    Value(String val) {
        type = "string";
        value = val;
    }

    Value(double val) {
        type = "double";
        value = Double.toString(val);
    }

    Value(long val) {
        type = "long";
        value = Long.toString(val);
    }

    Value(float val) {
        type = "long";
        value = Float.toString(val);
    }

    Value(int val) {
        type = "long";
        value = Integer.toString(val);
    }

    Value(Date val) {
        type = "date";
        value = Long.toString(val.getTime());
    }

    Value(boolean val) {
        type = "boolean";
        value = Boolean.toString(val);
    }
}
