package io.dwclark.btree;

import io.dwclark.btree.io.ImmutableBytes;
import io.dwclark.btree.io.MutableBytes;
import io.dwclark.btree.io.ViewBytes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

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
        this.root = viewBytes.withWrite((bytes) -> { return nextNode(bytes); }).node;
    }

    public static int bufferSizeForMinDegree(final int t) {
        return (((2 * t) - 1) * ENTRY_SIZE) + META_SIZE;
    }

    public static int findMinDegree(final int bufferSize) {
        if(bufferSize < bufferSizeForMinDegree(2)) {
            throw new IllegalArgumentException("not enough space for btree node");
        }

        int minDegree = 2;
        for(; bufferSizeForMinDegree(minDegree) <= bufferSize; ++minDegree) {}
        return minDegree -1;
    }
    
    protected MutableNode nextNode(final MutableBytes bytes) {
        return new MutableNode(bytes, (int) allocator.next()).leaf(true);
    }

    public class ImmutableNode<T extends ImmutableBytes> {
        int node;
        T bytes;
        long base;
        boolean leaf;
        int count;
        int index;
        long pos;
        
        public ImmutableNode(final T bytes, final int node) {
            this.bytes = bytes;
            init(node);
        }
        
        private ImmutableNode init(final int node) {
            this.node = node;
            this.base = ((long) (0xFFFF_FFFFL & node)) * ((long) bufferSize);
            this.count = bytes.readShort(base) & 0x7FFF;
            this.leaf = 0 != (bytes.readShort(base) & 0x8000);
            this.index = 0;
            this.pos = base + COUNT_SIZE;
            return this;
        }

        public ImmutableNode childNode() {
            return new ImmutableNode(bytes, child());
        }

        public ImmutableNode rightChildNode() {
            if(index == count()) {
                return null;
            }
            else {
                return new ImmutableNode(bytes, rightChild());
            }
        }

        public ImmutableNode leftChildNode() {
            return new ImmutableNode(bytes, leftChild());
        }

        public ImmutableNode index(int index) {
            this.index = index;
            this.pos = base + COUNT_SIZE + (index * ENTRY_SIZE);
            return this;
        }

        public ImmutableNode incrementIndex() {
            index += 1;
            pos += ENTRY_SIZE;
            return this;
        }

        public ImmutableNode decrementIndex() {
            index -= 1;
            pos -= ENTRY_SIZE;
            return this;
        }

        public int count() {
            return count;
        }

        public boolean leaf() {
            return leaf;
        }

        public int index() {
            return index;
        }

        public int child() {
            return bytes.readInt(pos);
        }

        public int leftChild() {
            return child();
        }

        public int rightChild() {
            return bytes.readInt(pos + ENTRY_SIZE);
        }

        public long key() {
            return bytes.readLong(pos + POINTER_SIZE);
        }

        public long value() {
            return bytes.readLong(pos + POINTER_SIZE + KEY_SIZE);
        }

        public boolean find(final long k) {
            for(; index < count && k > key(); ++index) {
                pos += (POINTER_SIZE + KEY_SIZE + VALUE_SIZE);
            }
            
            return index < count && k == key();
        }

        public List<Long> keys() {
            final List<Long> ret = new ArrayList<>();
            index(0);
            while(index < count) {
                ret.add(key());
                incrementIndex();
            }

            return ret;
        }

        public List<Long> values() {
            final List<Long> ret = new ArrayList<>();
            index(0);
            while(index < count) {
                ret.add(value());
                incrementIndex();
            }

            return ret;
        }
    }

    public class MutableNode extends ImmutableNode<MutableBytes> {
        MutableNode(final MutableBytes bytes, int node) {
            super(bytes, node);
        }

        @Override
        public MutableNode childNode() {
            return new MutableNode(bytes, child());
        }

        @Override
        public MutableNode leftChildNode(){
            return new MutableNode(bytes, leftChild());
        }

        public MutableNode leftSiblingNode() {
            if(index == 0) {
                return null;
            }
            else {
                return new MutableNode(bytes, bytes.readInt(pos - ENTRY_SIZE));
            }
        }

        @Override
        public MutableNode rightChildNode() {
            if(index == count()) {
                return null;
            }
            else {
                return new MutableNode(bytes, rightChild());
            }
        }

        

        public MutableNode leaf(final boolean val) {
            final short now = bytes.readShort(base);
            if(val) {
                bytes.writeShort(base, (short) (now | 0x8000));
            }
            else {
                bytes.writeShort(base, (short) (now & 0x7FFF));
            }

            this.leaf = val;
            return this;
        }

        public MutableNode count(final int val) {
            final short toWrite = (short) ((bytes.readShort(base) & 0x8000) | val);
            bytes.writeShort(base, toWrite);
            this.count = val;
            return this;
        }

        public MutableNode incrementCount() {
            return count(count + 1);
        }

        public MutableNode decrementCount() {
            return count(count - 1);
        }

        public MutableNode child(final int val) {
            bytes.writeInt(pos, val);
            return this;
        }

        public MutableNode leftChild(final int val) {
            return child(val);
        }
        
        public MutableNode rightChild(final int val) {
            bytes.writeInt(pos + ENTRY_SIZE, val);
            return this;
        }

        public MutableNode child(final ImmutableNode node) {
            return child(node.node);
        }

        public MutableNode leftChild(final ImmutableNode node) {
            return leftChild(node.node);
        }

        public MutableNode rightChild(final ImmutableNode node){
            return rightChild(node.node);
        }

        private int shiftLength() {
            return (ENTRY_SIZE * (count - index)) + POINTER_SIZE;
        }

        public MutableNode rightShift() {
            bytes.copy(pos + ENTRY_SIZE, bytes, pos, shiftLength());
            return this;
        }

        public MutableNode leftShift() {
            bytes.copy(pos - ENTRY_SIZE, bytes, pos, shiftLength());
            return this;
        }

        public MutableNode key(final long k) {
            bytes.writeLong(pos + POINTER_SIZE, k);
            return this;
        }

        public MutableNode value(final long v) {
            bytes.writeLong(pos + POINTER_SIZE + KEY_SIZE, v);
            return this;
        }
    }

    private long search(final ImmutableNode node, final long key) {
        if(node.find(key)) {
            return node.value();
        }
        else if(node.leaf()) {
            return -1L;
        }
        else {
            return search(node.childNode(), key);
        }
    }
    
    public long search(final long key) {
        return viewBytes.withRead((bytes) -> { return search(new ImmutableNode(bytes, root), key); });
    }

    private void split(final MutableNode parent) {
        final int restoreIndex = parent.index;
        final MutableBytes bytes = parent.bytes;
        final MutableNode child = (MutableNode) parent.childNode().index(minKeys);
        final MutableNode sibling = nextNode(bytes);
        final long key = child.key();
        final long value = child.value();

        child.incrementIndex();
        bytes.copy(sibling.pos, bytes, child.pos, (minKeys * ENTRY_SIZE) + POINTER_SIZE);
        
        sibling.leaf(child.leaf()).count(minKeys);
        
        child.count(minKeys);
        
        parent.rightShift().child(child).key(key).value(value)
            .incrementCount().incrementIndex();
        parent.child(sibling);
        parent.index(restoreIndex);
    }

    private void merge(final MutableNode parent) {
        final MutableBytes bytes = parent.bytes;
        final MutableNode leftChild = parent.leftChildNode();
        final MutableNode rightChild = parent.rightChildNode();

        if(leftChild.count() != minKeys || rightChild.count() != minKeys) {
            throw new IllegalStateException("attempting illegal merge");
        }

        //copy parent value in
        leftChild.index(minKeys);
        leftChild.key(parent.key());
        leftChild.value(parent.value());
        leftChild.incrementIndex();

        //move right child into left child, deallocate right child
        bytes.copy(leftChild.pos, bytes, rightChild.pos, (minKeys * ENTRY_SIZE) + POINTER_SIZE);
        leftChild.count(1 + (2 * minKeys));
        allocator.unused(rightChild.node);

        //remove value from parent
        parent.incrementIndex();
        parent.leftShift().decrementCount().decrementIndex();
        parent.leftChild(leftChild);
    }

    private void insertNotFull(final MutableNode node, final long key, final long value) {
        //if is update, can always safely perform
        if(node.find(key)) {
            node.value(value);
            return;
        }
        
        if(node.leaf()) {
            //guaranteed to not be full because we have pre-split if necessary
            node.rightShift().key(key).value(value).incrementCount();
        }
        else {
            //split if at max
            if(node.childNode().count() == maxKeys) {
                split(node);
                //after split, we may need to re-position index
                //so that we choose the correct child node
                node.find(key);
            }

            insertNotFull(node.childNode(), key, value);
        }
    }

    private void insert(final MutableNode rootNode, final long key, final long value) {
        if(rootNode.count() == maxKeys) {
            //root is full, need to split it and then call insertNonFull on the new root
            final MutableNode newRoot = nextNode(rootNode.bytes);
            this.root = newRoot.node;
            newRoot.leaf(false);
            newRoot.count(0);
            newRoot.child(rootNode);
            split(newRoot);
            insertNotFull(newRoot, key, value);
        }
        else {
            insertNotFull(rootNode, key, value);
        }
    }
    
    public void insert(final long key, final long value) {
        viewBytes.withWrite((bytes) -> { insert(new MutableNode(bytes, root), key, value); });
    }

    private boolean removeLeaf(final MutableNode node, final long key) {
        if(node.find(key)) {
            node.incrementIndex();
            node.leftShift().decrementCount();
            return true;
        }
        else {
            return false;
        }
    }

    private void removeInnerNode(final MutableNode node, final long key) {
        final MutableNode left = node.leftChildNode();
        final MutableNode right = node.rightChildNode();

        if(left.count() >= minDegree) {
            left.index(left.count() - 1);
            final long predecessorKey = left.key();
            final long predecessorValue = left.value();
            remove(left, predecessorKey);
            node.key(predecessorKey);
            node.value(predecessorValue);
        }
        else if(right.count() >= minDegree) {
            right.index(0);
            final long successorKey = right.key();
            final long successorValue = right.value();
            remove(right, successorKey);
            node.key(successorKey);
            node.value(successorValue);
        }
        else {
            merge(node);
            node.find(key);
            remove(node.leftChildNode(), key);
        }
    }

    private void fixUpChildren(final MutableNode parent, final long key) {
        final MutableNode child = parent.leftChildNode();
        final MutableNode leftSibling = parent.leftSiblingNode();
        final MutableNode rightSibling = parent.rightChildNode();
        if(leftSibling != null && leftSibling.count() > minKeys) {
            leftSibling.index(leftSibling.count() - 1);
            final long leftKey = leftSibling.key();
            final long leftValue = leftSibling.value();
            leftSibling.incrementIndex();
            leftSibling.leftShift().decrementCount();

            parent.decrementIndex();
            child.rightShift().key(parent.key()).value(parent.value()).incrementCount();
            parent.key(leftKey).value(leftValue);
            parent.incrementIndex();
        }
        else if(rightSibling != null && rightSibling.count() > minKeys) {
            final long rightKey = rightSibling.key();
            final long rightValue = rightSibling.value();
            rightSibling.incrementIndex();
            rightSibling.leftShift().decrementCount();

            child.index(child.count());
            child.rightShift().key(parent.key()).value(parent.value()).incrementCount();

            parent.key(rightKey).value(rightValue);
        }
        else if(leftSibling != null) {
            parent.decrementIndex();
            merge(parent);
            parent.find(key);
        }
        else {
            merge(parent);
            parent.find(key);
        }
    }
    
    private boolean remove(final MutableNode node, final long key) {
        if(node.leaf()) {
            return removeLeaf(node, key);
        }
        else {
            if(node.find(key)) {
                removeInnerNode(node, key);
                return true;
            }

            final MutableNode child = node.leftChildNode();
            if(child.count() > minKeys) {
                return remove(child, key);
            }

            fixUpChildren(node, key);
            return remove(node.leftChildNode(), key);
        }
    }

    public boolean remove(final long key) {
        final Function<MutableBytes,Boolean> func = (MutableBytes bytes) -> {
            final MutableNode rootNode = new MutableNode(bytes, root);
            final Boolean ret = Boolean.valueOf(remove(rootNode, key));
            if(rootNode.count() == 0) {
                this.root = rootNode.leftChild();
                allocator.unused(rootNode.node);
            }

            return ret;
        };
        
        return viewBytes.withWrite(func).booleanValue();
    }

    private void toString(final StringBuilder sb, final ImmutableNode node) {
        sb.append("Node: ").append(node.node).append(" ");
        while(node.index() < node.count()) {
            sb.append("{");
            if(!node.leaf()) {
                sb.append(node.child()).append(",");
            }
            
            sb.append(node.key()).append(",");
            sb.append(node.value()).append("} ");
            node.incrementIndex();
        }

        if(!node.leaf()) {
            sb.append("{").append(node.child()).append("}; ");

            node.index(0);
            while(node.index() <= node.count()) {
                toString(sb, node.childNode());
                node.incrementIndex();
            }
        }
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        viewBytes.withRead((bytes) -> { toString(sb, new ImmutableNode(bytes, root)); });
        return sb.toString();
    }

    private List<ImmutableNode> _breadthFirst(final ImmutableBytes bytes) {
        final List<ImmutableNode> ret = new ArrayList<>();
        final Queue<Integer> traversal = new LinkedList<>();
        traversal.offer(root);
        while(!traversal.isEmpty()) {
            final ImmutableNode node = new ImmutableNode(bytes, traversal.poll());
            ret.add(node);
            if(!node.leaf()) {
                while(node.index <= node.count) {
                    traversal.offer(node.leftChild());
                    node.incrementIndex();
                }
            }
        }

        return ret;
    }
    
    public List<ImmutableNode> breadthFirst() {
        return viewBytes.withRead(this::_breadthFirst);
    }

    protected MutableNode mutableRoot() {
        return new MutableNode(viewBytes.forWrite(), root);
    }

    protected ImmutableNode immutableRoot() {
        return new ImmutableNode(viewBytes.forRead(), root);
    }
}
