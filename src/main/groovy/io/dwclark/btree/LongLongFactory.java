package io.dwclark.btree;

import io.dwclark.btree.io.ImmutableBytes;
import io.dwclark.btree.io.MutableBytes;

public class LongLongFactory implements NodeFactory<Long,Long> {

    private final int bufferSize;
    private final int minDegree;
    
    public LongLongFactory(final int bufferSize) {
        this.bufferSize = bufferSize;
        this.minDegree = findMinDegree();
    }
    
    public final LongRecord keyRecord() { return LongRecord.instance(); }
    public final LongRecord valueRecord() { return LongRecord.instance(); }
    public final int getBufferSize() { return bufferSize; }
    public final int getMinDegree() { return minDegree; }
    
    public Node.Mutable mutable(final MutableBytes bytes, final int node) {
        return new Node.Mutable(bytes, node, bufferSize);
    }
    
    public Node.Immutable immutable(final ImmutableBytes bytes, final int node) {
        return new Node.Immutable(bytes, node, bufferSize);
    }
}
