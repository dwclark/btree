package io.dwclark.btree;

import io.dwclark.btree.io.ImmutableBytes;
import io.dwclark.btree.io.MutableBytes;

public class StandardFactory<K,V> implements NodeFactory<K,V> {

    private final int minDegree;
    private final int bufferSize;
    private final Record<K> keyRecord;
    private final Record<V> valueRecord;

    public StandardFactory(final Record<K> keyRecord, final Record<V> valueRecord, final int bufferSize) {
        this.keyRecord = keyRecord;
        this.valueRecord = valueRecord;
        this.bufferSize = bufferSize;
        this.minDegree = findMinDegree();
    }

    public final int findMinDegree() {
        return NodeFactory.super.findMinDegree();
    }

    public final Record<K> keyRecord() { return keyRecord; }
    public final Record<V> valueRecord() { return valueRecord; }
    public final int getBufferSize(){ return bufferSize; }
    public final int getMinDegree() { return minDegree; }
}
