package io.dwclark.btree;

import io.dwclark.btree.io.ImmutableBytes;
import io.dwclark.btree.io.MutableBytes;

interface NodeFactory<K,V> {
    Record<K> keyRecord();
    Record<V> valueRecord();
    int getBufferSize();
    int getMinDegree();
    Node.Mutable mutable(MutableBytes bytes, int node);
    Node.Immutable immutable(ImmutableBytes bytes, int node);

    public static <K,V> int bufferSizeForMinDegree(final int t, final Record<K> keyRecord, final Record<V> valueRecord) {
        return (((2 * t) - 1) * Node.entrySize(keyRecord, valueRecord)) + Node.META_SIZE;
    }

    default int findMinDegree() {
        if(getBufferSize() < bufferSizeForMinDegree(2, keyRecord(), valueRecord())) {
            throw new IllegalArgumentException("not enough space for btree node");
        }

        int minDegree = 2;
        for(; bufferSizeForMinDegree(minDegree, keyRecord(), valueRecord()) <= getBufferSize(); ++minDegree) {}
        return minDegree -1;
    }

    default int getMinKeys() { return (getMinDegree() - 1); }
    default int getMinChildren() { return getMinDegree(); }
    default int getMaxKeys() { return ((2 * getMinDegree()) - 1); }
    default int getMaxChildren() { return (2 * getMinDegree()); }

}
