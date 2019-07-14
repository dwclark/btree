package io.dwclark.btree;

import io.dwclark.btree.io.ImmutableBytes;
import io.dwclark.btree.io.MutableBytes;

interface VariableRecord<T> {
    T extract(ImmutableBytes bytes, long pos, int size);
    int place(MutableBytes bytes, long pos, T val);
}
