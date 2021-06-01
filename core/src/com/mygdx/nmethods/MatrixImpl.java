package com.mygdx.nmethods;

import java.util.AbstractList;
import java.util.List;
import java.util.stream.Collectors;

public class MatrixImpl extends AbstractList<Vector> implements DoubleMatrix {
    private final List<Vector> a;

    public MatrixImpl(final List<List<Double>> a) {
        this.a = a.stream().map(Vector::new).collect(Collectors.toList());
        if (a.stream().anyMatch(list -> list.size() != a.size())) {
            throw new IllegalArgumentException("not quadratic matrix");
        }
    }

    public double get(final int r, final int c) {
        return a.get(r).get(c);
    }

    @Override
    public Vector get(final int index) {
        return a.get(index);
    }

    @Override
    public int size() {
        return a.size();
    }
}
