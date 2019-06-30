package io.dwclark.btree;

import io.dwclark.btree.io.ViewBytes;
import io.dwclark.btree.io.ImmutableBytes;
import io.dwclark.btree.io.MutableBytes;

public class StandardBTree {

    private static final int COUNT_SIZE = 2;
    private static final int POINTER_SIZE = 4;
    private static final int META_SIZE = COUNT_SIZE + POINTER_SIZE;
    private static final int KEY_SIZE = 8;
    private static final int VALUE_SIZE = 8;
    private static final int ENTRY_SIZE = POINTER_SIZE + KEY_SIZE + VALUE_SIZE;

    private final ViewBytes viewBytes;
    private final BlockAllocator allocator;
    private final int bufferSize;
    private final int minDegree;
    private final int minKeys;
    private final int minChildren;
    private final int maxKeys;
    private final int maxChildren;
    
    private int root;
    
    public int getBufferSize() { return bufferSize; }
    public int getMinDegree() { return minDegree; }
    public int getMinKeys() { return minKeys; }
    public int getMinChildren() { return minChildren; }
    public int getMaxKeys() { return maxKeys; }
    public int getMaxChildren() { return maxChildren; }
    public int getRoot() { return root; }
    public long getRootBase() { return ((long) bufferSize) * root; }

    public StandardBTree(final int bufferSize, final int root,
                         final ViewBytes viewBytes, final BlockAllocator allocator) {
        this.bufferSize = bufferSize;
        this.viewBytes = viewBytes;
        this.allocator = allocator;
        this.root = root;

        this.minDegree = findMinDegree(bufferSize);
        this.minKeys = minDegree - 1;
        this.minChildren = minDegree;
        this.maxKeys = (2 * minDegree) - 1;
        this.maxChildren = 2 * minDegree;
    }
    
    public StandardBTree(final int bufferSize, final ViewBytes viewBytes) {
        this(bufferSize, 0, viewBytes, new BlockAllocator(0xFFFF_FFFFL, false));
        this.root = nextBlock(); //initialize root
    }

    public static int findMinDegree(final int bufferSize) {
        final int possible = (bufferSize - META_SIZE) / (ENTRY_SIZE * 2);
        final int actual = possible % 2 == 0 ? possible : possible - 1;

        if(actual < 2) {
            throw new IllegalArgumentException("insufficient size for b-tree operations");
        }

        return actual;
    }
    
    private int nextBlock() {
        return viewBytes.withWrite((bytes) -> {
                final int at = (int) allocator.next();
                final long base = base(at);
                leaf(bytes, base, true);
                return (int) at;
            });
    }

    private long base(final int node) {
        return base(node, bufferSize);
    }

    public static long base(final int node, final int bufferSize){
        return ((long) (0xFFFF_FFFFL & node)) * ((long) bufferSize);
    }

    public static int count(final ImmutableBytes bytes, final long base) {
        return bytes.readShort(base) & 0x7FFF;
    }

    public static void count(final MutableBytes bytes, final long base, final int val) {
        final short toWrite = (short) ((bytes.readShort(base) & 0x8000) | val);
        bytes.writeShort(base, toWrite);
    }

    public static boolean leaf(final ImmutableBytes bytes, final long base) {
        return 0 != (bytes.readShort(base) & 0x7FFFF);
    }

    public static void leaf(final MutableBytes bytes, final long base, final boolean val) {
        final short now = bytes.readShort(base);
        if(val) {
            bytes.writeShort(base, (short) (now | 0x8000));
        }
        else {
            bytes.writeShort(base, (short) (now & 0x7FFF));
        }
    }

    public static long recordPosition(final long base, final int i) {
        return base + COUNT_SIZE + (i * ENTRY_SIZE);
    }

    public static int child(final ImmutableBytes bytes, final long base, final int i) {
        return bytes.readInt(recordPosition(base, i));
    }

    public static void child(final MutableBytes bytes, final long base, final int i, final int val) {
        bytes.writeInt(recordPosition(base, i), val);
    }

    public static long key(final ImmutableBytes bytes, final long base, final int i) {
        return bytes.readLong(recordPosition(base, i) + POINTER_SIZE);
    }

    public static void key(final MutableBytes bytes, final long base, final int i, final long val) {
        bytes.writeLong(recordPosition(base, i) + POINTER_SIZE, val);
    }

    public static long value(final ImmutableBytes bytes, final long base, final int i) {
        return bytes.readLong(recordPosition(base, i) + POINTER_SIZE + KEY_SIZE);
    }

    public static void value(final MutableBytes bytes, final long base, final int i, final long val) {
        bytes.writeLong(recordPosition(base, i) + POINTER_SIZE + KEY_SIZE, val);
    }
    
    private long search(final ImmutableBytes bytes, final int node, final long key) {
        final long base = base(node);
        final int count = count(bytes, base);
        int i = 0;

        for(; i < count && key > key(bytes, base, i); ++i) {}
        
        if(i < count && key == key(bytes, base, i)) {
            return value(bytes, base, i);
        }

        if(leaf(bytes, base)) {
            return -1L;
        }

        return search(bytes, child(bytes, base, i), key);
    }
    
    public long search(final long key, final long satellite) {
        return viewBytes.withRead((bytes) -> { return search(bytes, root, key); });
    }

    public static void shiftRecord(final MutableBytes bytes, final long base, final int index) {
        final int count = count(bytes, base);
        final long srcAt = recordPosition(base, index);
        final int moveLength = (int) ((recordPosition(base, count) + POINTER_SIZE) - srcAt);
        final long targetAt = srcAt + ENTRY_SIZE;
        bytes.copy(targetAt, bytes, srcAt, moveLength);
    }

    public static void placeNew(final MutableBytes bytes, final long base, final int index,
                                final long key, final long value) {
        final long pos = recordPosition(base, index);
        bytes.writeLong(pos + POINTER_SIZE, key);
        bytes.writeLong(pos + POINTER_SIZE + KEY_SIZE, value);
        final int current = count(bytes, base);
        count(bytes, base, current + 1);
    }

    private void split(final MutableBytes bytes, final int parent, final int parentIndex, final int self) {
        final int newNode = nextBlock();
        final long newBase = base(newNode);
        final long selfBase = base(self);
        final int selfIndex = minKeys;
        final long selfKey = key(bytes, selfBase, selfIndex);
        final long selfValue = value(bytes, selfBase, selfIndex);
        final long parentBase = base(parent);
        final int parentCount = count(bytes, parentBase);

        //move upper half of data into new node
        leaf(bytes, newBase, leaf(bytes, self));
        count(bytes, newBase, minKeys);
        bytes.copy(recordPosition(selfBase, selfIndex + 1),
                   bytes, newBase, (minKeys * ENTRY_SIZE) + POINTER_SIZE);

        //set count on self node to min
        count(bytes, selfBase, minKeys);

        //move parent right by entry size
        shiftRecord(bytes, parentBase, parentIndex);

        //place new and set children
        placeNew(bytes, parentBase, parentIndex, selfKey, selfValue);
        child(bytes, parentBase, parentIndex, self);
        child(bytes, parentBase, parentIndex + 1, newNode);
    }

    private void insertNotFull(final MutableBytes bytes, final int node, final long key, final long value) {
        final long nodeBase = base(node);
        final int nodeCount = count(bytes, nodeBase);

        //find correct index
        int index = 0;
        for(; index < nodeCount && key > key(bytes, nodeBase, index); ++index) {}

        //if is update, can always safely perform
        if(key == key(bytes, nodeBase, index)) {
            value(bytes, nodeBase, index, value);
            return;
        }
        
        if(leaf(bytes, nodeBase)) {
            //guaranteed to not be full because we have pre-split if necessary
            shiftRecord(bytes, nodeBase, index);
            placeNew(bytes, nodeBase, index, key, value);
        }
        else {
            //split child; once split happens, determine location
            //of new insertion and recurse
            final int childNode = child(bytes, nodeBase, index);
            final long childBase = base(childNode);
            if(count(bytes, childBase) == maxKeys) {
                split(bytes, node, index, childNode);
                if(key > key(bytes, nodeBase, index)) {
                    ++index;
                }
                
                insertNotFull(bytes, child(bytes, nodeBase, index), key, value);
            }
        }
    }

    private void insert(final MutableBytes bytes, final int node, final long key, final long value) {
        if(count(bytes, base(node)) == maxKeys) {
            //root is full, need to split it and then call insertNonFull on the new root
            final int newNode = nextBlock();
            this.root = newNode;
            final long newNodeBase = base(newNode);
            leaf(bytes, newNodeBase, false);
            count(bytes, newNodeBase, 0);
            child(bytes, newNodeBase, 0, node);
            split(bytes, newNode, 0, node);
            insertNotFull(bytes, newNode, key, value);
        }
        else {
            //root isn't full, proceed with non full insert
            insertNotFull(bytes, node, key, value);
        }
    }

    public void insert(final long key, final long value) {
        viewBytes.withWrite((bytes) -> { insert(bytes, root, key, value); });
    }
    
    public void remove(final long key) {

    }
}
        
