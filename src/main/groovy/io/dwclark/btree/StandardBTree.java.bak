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
        return (((2 * t) - 1) * Node.entrySize(LongRecord.instance(), LongRecord.instance())) + Node.META_SIZE;
    }

    public static int findMinDegree(final int bufferSize) {
        if(bufferSize < bufferSizeForMinDegree(2)) {
            throw new IllegalArgumentException("not enough space for btree node");
        }

        int minDegree = 2;
        for(; bufferSizeForMinDegree(minDegree) <= bufferSize; ++minDegree) {}
        return minDegree -1;
    }
    
    protected Node.Mutable nextNode(final MutableBytes bytes) {
        return new Node.Mutable(bytes, (int) allocator.next(), bufferSize).leaf(true);
    }

    private long search(final Node.Immutable node, final long key) {
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
        return viewBytes.withRead((bytes) -> {
                return search(new Node.Immutable(bytes, root, bufferSize), key); });
    }

    private void split(final Node.Mutable parent) {
        final int restoreIndex = parent.index;
        final MutableBytes bytes = parent.bytes;
        final Node.Mutable child = (Node.Mutable) parent.childNode().index(minKeys);
        final Node.Mutable sibling = nextNode(bytes);
        final long key = child.key();
        final long value = child.value();

        child.incrementIndex();
        bytes.copy(sibling.pos(), bytes, child.pos(),
                   (minKeys * parent.entrySize()) + Node.CHILD_SIZE);
        
        sibling.leaf(child.leaf()).count(minKeys);
        
        child.count(minKeys);
        
        parent.rightShift().child(child).key(key).value(value)
            .incrementCount().incrementIndex();
        parent.child(sibling);
        parent.index(restoreIndex);
    }

    private void merge(final Node.Mutable parent) {
        final MutableBytes bytes = parent.bytes;
        final Node.Mutable leftChild = parent.leftChildNode();
        final Node.Mutable rightChild = parent.rightChildNode();

        if(leftChild.count() != minKeys || rightChild.count() != minKeys) {
            throw new IllegalStateException("attempting illegal merge");
        }

        //copy parent value in
        leftChild.index(minKeys);
        leftChild.key(parent.key());
        leftChild.value(parent.value());
        leftChild.incrementIndex();

        //move right child into left child, deallocate right child
        bytes.copy(leftChild.pos(), bytes, rightChild.pos(),
                   (minKeys * parent.entrySize()) + Node.CHILD_SIZE);
        leftChild.count(1 + (2 * minKeys));
        allocator.unused(rightChild.node);

        //remove value from parent
        parent.incrementIndex();
        parent.leftShift().decrementCount().decrementIndex();
        parent.leftChild(leftChild);
    }

    private void insertNotFull(final Node.Mutable node, final long key, final long value) {
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

    private void insert(final Node.Mutable rootNode, final long key, final long value) {
        if(rootNode.count() == maxKeys) {
            //root is full, need to split it and then call insertNonFull on the new root
            final Node.Mutable newRoot = nextNode(rootNode.bytes);
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
        viewBytes.withWrite((bytes) -> { insert(new Node.Mutable(bytes, root, bufferSize), key, value); });
    }

    private boolean removeLeaf(final Node.Mutable node, final long key) {
        if(node.find(key)) {
            node.incrementIndex();
            node.leftShift().decrementCount();
            return true;
        }
        else {
            return false;
        }
    }

    private void removeInnerNode(final Node.Mutable node, final long key) {
        final Node.Mutable left = node.leftChildNode();
        final Node.Mutable right = node.rightChildNode();

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

    private void fixUpChildren(final Node.Mutable parent, final long key) {
        final Node.Mutable child = parent.leftChildNode();
        final Node.Mutable leftSibling = parent.leftSiblingNode();
        final Node.Mutable rightSibling = parent.rightChildNode();
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
    
    private boolean remove(final Node.Mutable node, final long key) {
        if(node.leaf()) {
            return removeLeaf(node, key);
        }
        else {
            if(node.find(key)) {
                removeInnerNode(node, key);
                return true;
            }

            final Node.Mutable child = node.leftChildNode();
            if(child.count() > minKeys) {
                return remove(child, key);
            }

            fixUpChildren(node, key);
            return remove(node.leftChildNode(), key);
        }
    }

    public boolean remove(final long key) {
        final Function<MutableBytes,Boolean> func = (MutableBytes bytes) -> {
            final Node.Mutable rootNode = new Node.Mutable(bytes, root, bufferSize);
            final Boolean ret = Boolean.valueOf(remove(rootNode, key));
            if(rootNode.count() == 0) {
                this.root = rootNode.leftChild();
                allocator.unused(rootNode.node);
            }

            return ret;
        };
        
        return viewBytes.withWrite(func).booleanValue();
    }

    private void toString(final StringBuilder sb, final Node.Immutable node) {
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
        viewBytes.withRead((bytes) -> { toString(sb, new Node.Immutable(bytes, root, bufferSize)); });
        return sb.toString();
    }

    private List<Node.Immutable> _breadthFirst(final ImmutableBytes bytes) {
        final List<Node.Immutable> ret = new ArrayList<>();
        final Queue<Integer> traversal = new LinkedList<>();
        traversal.offer(root);
        while(!traversal.isEmpty()) {
            final Node.Immutable node = new Node.Immutable(bytes, traversal.poll(), bufferSize);
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
    
    public List<Node.Immutable> breadthFirst() {
        return viewBytes.withRead(this::_breadthFirst);
    }

    protected Node.Mutable mutableRoot() {
        return new Node.Mutable(viewBytes.forWrite(), root, bufferSize);
    }

    protected Node.Immutable immutableRoot() {
        return new Node.Immutable(viewBytes.forRead(), root, bufferSize);
    }
}
