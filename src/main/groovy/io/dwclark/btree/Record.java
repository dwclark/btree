package io.dwclark.btree;

import io.dwclark.btree.io.ImmutableBytes;
import io.dwclark.btree.io.MutableBytes;

interface Record<T> {
    int size();
    T extract(ImmutableBytes bytes, long pos);
    void place(MutableBytes bytes, long pos, T val);
    int compare(ImmutableBytes bytes, long pos, T val);
}
