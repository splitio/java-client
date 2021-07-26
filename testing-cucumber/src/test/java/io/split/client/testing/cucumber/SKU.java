package io.split.client.testing.cucumber;

import java.util.Objects;

/**
 * A simple <a href="https://en.wikipedia.org/wiki/Stock_keeping_unit">Stock Keeping Unit</a> (SKU).
 */
public class SKU {
    private final String name;
    private final double price;

    public SKU(String name, double price) {
        this.name = name;
        this.price = price;
    }

    @Override
    public String toString() {
        return "SKU{" +
                "name='" + name + '\'' +
                ", price=" + price +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SKU sku = (SKU) o;
        return Double.compare(sku.price, price) == 0 && name.equals(sku.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, price);
    }
}
