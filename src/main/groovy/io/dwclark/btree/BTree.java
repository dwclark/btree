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

public class BTree<K,V> {

    private final ViewBytes viewBytes;
    private final BlockAllocator allocator;
    private final NodeFactory<K,V> factory;

    public int getRoot() { return root; }
    
    private int root;

    public BTree(final ViewBytes viewBytes, final BlockAllocator allocator,
                 final NodeFactory<K,V> factory, final int root) {
        this.viewBytes = viewBytes;
        this.allocator = allocator;
        this.factory = factory;
        this.root = root;
    }
    
    public BTree(final ViewBytes viewBytes, final NodeFactory<K,V> factory) {
        this(viewBytes, new BlockAllocator(0xFFFF_FFFFL, false), factory, 0);
        this.root = viewBytes.withWrite((bytes) -> { return nextNode(bytes); }).node();
    }
    
    protected Node.Mutable<K,V> nextNode(final MutableBytes bytes) {
        return factory.mutable(bytes, (int) allocator.next()).leaf(true);
    }

    private V search(final Node.Immutable<K,V> node, final K key) {
        if(node.find(key)) {
            return node.value();
        }
        else if(node.leaf()) {
            return null;
        }
        else {
            return search(node.childNode(), key);
        }
    }
    
    public V search(final K key) {
        return viewBytes.withRead((bytes) -> { return search(factory.immutable(bytes, root), key); });
    }
    
    private void split(final Node.Mutable<K,V> parent) {
        final int restoreIndex = parent.index();
        final MutableBytes bytes = parent.bytes();
        final int minKeys = factory.getMinKeys();
        final Node.Mutable<K,V> child = (Node.Mutable<K,V>) parent.childNode().index(minKeys);
        final Node.Mutable<K,V> sibling = nextNode(bytes);
        final K key = child.key();
        final V value = child.value();

        child.incrementIndex();
        bytes.copy(sibling.pos(), bytes, child.pos(),
                   (minKeys * parent.entrySize()) + Node.CHILD_SIZE);
        
        sibling.leaf(child.leaf()).count(minKeys);
        
        child.count(minKeys);
        
        parent.rightShift().child(child).key(key).value(value).incrementCount().incrementIndex();
        parent.child(sibling);
        parent.index(restoreIndex);
    }

    private void merge(final Node.Mutable<K,V> parent) {
        final MutableBytes bytes = parent.bytes();
        final int minKeys = factory.getMinKeys();
        final Node.Mutable<K,V> leftChild = parent.leftChildNode();
        final Node.Mutable<K,V> rightChild = parent.rightChildNode();

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
        allocator.unused(rightChild.node());

        //remove value from parent
        parent.incrementIndex();
        parent.leftShift().decrementCount().decrementIndex();
        parent.leftChild(leftChild);
    }

    private void insertNotFull(final Node.Mutable<K,V> node, final K key, final V value) {
        final int maxKeys = factory.getMaxKeys();
        
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

    private void insert(final Node.Mutable<K,V> rootNode, final K key, final V value) {
        final int maxKeys = factory.getMaxKeys();
        
        if(rootNode.count() == maxKeys) {
            //root is full, need to split it and then call insertNonFull on the new root
            final Node.Mutable<K,V> newRoot = nextNode(rootNode.bytes);
            this.root = newRoot.node();
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
    
    public void insert(final K key, final V value) {
        viewBytes.withWrite((bytes) -> { insert(factory.mutable(bytes, root), key, value); });
    }

    private boolean removeLeaf(final Node.Mutable<K,V> node, final K key) {
        if(node.find(key)) {
            node.incrementIndex();
            node.leftShift().decrementCount();
            return true;
        }
        else {
            return false;
        }
    }

    private void removeInnerNode(final Node.Mutable<K,V> node, final K key) {
        final int minDegree = factory.getMinDegree();
        final Node.Mutable<K,V> left = node.leftChildNode();
        final Node.Mutable<K,V> right = node.rightChildNode();

        if(left.count() >= minDegree) {
            left.index(left.count() - 1);
            final K predecessorKey = left.key();
            final V predecessorValue = left.value();
            remove(left, predecessorKey);
            node.key(predecessorKey);
            node.value(predecessorValue);
        }
        else if(right.count() >= minDegree) {
            right.index(0);
            final K successorKey = right.key();
            final V successorValue = right.value();
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

    private void fixUpChildren(final Node.Mutable<K,V> parent, final K key) {
        final int minKeys = factory.getMinKeys();
        final Node.Mutable<K,V> child = parent.leftChildNode();
        final Node.Mutable<K,V> leftSibling = parent.leftSiblingNode();
        final Node.Mutable<K,V> rightSibling = parent.rightChildNode();
        if(leftSibling != null && leftSibling.count() > minKeys) {
            leftSibling.index(leftSibling.count() - 1);
            final K leftKey = leftSibling.key();
            final V leftValue = leftSibling.value();
            leftSibling.incrementIndex();
            leftSibling.leftShift().decrementCount();

            parent.decrementIndex();
            child.rightShift().key(parent.key()).value(parent.value()).incrementCount();
            parent.key(leftKey).value(leftValue);
            parent.incrementIndex();
        }
        else if(rightSibling != null && rightSibling.count() > minKeys) {
            final K rightKey = rightSibling.key();
            final V rightValue = rightSibling.value();
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
    
    private boolean remove(final Node.Mutable<K,V> node, final K key) {
        final int minKeys = factory.getMinKeys();
        
        if(node.leaf()) {
            return removeLeaf(node, key);
        }
        else {
            if(node.find(key)) {
                removeInnerNode(node, key);
                return true;
            }

            final Node.Mutable<K,V> child = node.leftChildNode();
            if(child.count() > minKeys) {
                return remove(child, key);
            }

            fixUpChildren(node, key);
            return remove(node.leftChildNode(), key);
        }
    }

    public boolean remove(final K key) {
        final Function<MutableBytes,Boolean> func = (MutableBytes bytes) -> {
            final Node.Mutable<K,V> rootNode = factory.mutable(bytes, root);
            final Boolean ret = Boolean.valueOf(remove(rootNode, key));
            if(rootNode.count() == 0) {
                this.root = rootNode.leftChild();
                allocator.unused(rootNode.node());
            }

            return ret;
        };
        
        return viewBytes.withWrite(func).booleanValue();
    }

    private void toString(final StringBuilder sb, final Node.Immutable<K,V> node) {
        sb.append("Node: ").append(node.node()).append(" ");
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
        viewBytes.withRead((bytes) -> { toString(sb, factory.immutable(bytes, root)); });
        return sb.toString();
    }

    private List<Node.Immutable<K,V>> _breadthFirst(final ImmutableBytes bytes) {
        final List<Node.Immutable<K,V>> ret = new ArrayList<>();
        final Queue<Integer> traversal = new LinkedList<>();
        traversal.offer(root);
        while(!traversal.isEmpty()) {
            final Node.Immutable<K,V> node = factory.immutable(bytes, traversal.poll());
            ret.add(node);
            if(!node.leaf()) {
                while(node.index() <= node.count()) {
                    traversal.offer(node.leftChild());
                    node.incrementIndex();
                }
            }
        }

        return ret;
    }
    
    public List<Node.Immutable<K,V>> breadthFirst() {
        return viewBytes.withRead(this::_breadthFirst);
    }

    protected Node.Mutable<K,V> mutableRoot() {
        return factory.mutable(viewBytes.forWrite(), root);
    }

    protected Node.Immutable<K,V> immutableRoot() {
        return factory.immutable(viewBytes.forRead(), root);
    }
}
