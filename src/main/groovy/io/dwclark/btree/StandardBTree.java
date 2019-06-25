package io.dwclark.btree;

import io.dwclark.btree.io.ViewBytes;
import io.dwclark.btree.io.ImmutableBytes;
import io.dwclark.btree.io.MutableBytes;
public class StandardBTree {

    private static final int HEADER_SIZE = 2;

    private final ViewBytes viewBytes;
    private final int bufferSize;
    private final int max;
    private final int min;

    private int numberNodes;

    public int getBufferSize() { return bufferSize; }
    public int getMax() { return max; }
    public int getMin() { return min; }
    public int getNumberNodes() { return numberNodes; }

    public StandardBTree(final int bufferSize, final ViewBytes viewBytes, final int numberNodes) {
        this.bufferSize = bufferSize;
        this.viewBytes = viewBytes;
        this.max = (bufferSize - 6) / 20;
        this.min = max / 2;
        this.numberNodes = numberNodes;

        if(numberNodes == 0) {
            initialize();
        }
    }
    
    public StandardBTree(final int bufferSize, final ViewBytes viewBytes) {
        this(bufferSize, viewBytes, 0);
    }

    private void initialize() {
        final MutableBytes bytes = viewBytes.forWrite();
        bytes.writeShort(0L, (short) 0x8000);
        numberNodes = 1;
        bytes.stop();
    }

    private class Node {
        private ImmutableBytes bytes;
        private long base;
        private boolean leaf;
        private int count;
        private long baseData;
        private int index;
        
        public Node(final ImmutableBytes bytes) {
            this.bytes = bytes;
            setFor(0);
        }
        
        final Node setFor(final int node) {
            this.base = ((long) node) * ((long) bufferSize);
            final short first = bytes.readShort(base);
            this.leaf = first < 0;
            this.count = 0x7FFF & first;
            this.baseData = this.base + 2L;
            this.index = 0;
            return this;
        }

        public int child() {
            return bytes.readInt(baseData + (index * 20));
        }
        
        public long key() {
            return bytes.readLong(baseData + (index*20) + 4);
        }

        public long satellite() {
            return bytes.readLong(baseData + (index*20) + 12);
        }

        public boolean isLeaf() {
            return leaf;
        }

        public void next() {
            ++index;
        }

        public boolean hasNext() {
            return index < count;
        }
    }

    private long _search(final Node node, final long key) {
        while(node.hasNext() && key > node.key()) {
            node.next();
        }

        if(node.hasNext() && key == node.key()) {
            return node.satellite();
        }

        if(node.isLeaf()) {
            return -1L;
        }

        return _search(node.setFor(node.child()), key);
    }
    
    public long search(final long key) {
        final ImmutableBytes bytes = viewBytes.forRead();
        final long found = _search(new Node(bytes), key);
        bytes.stop();
        return found;
    }

    public void insert(final long key, final long value) {
        
    }
    
    public void remove(final long key) {

    }
}
        
