package io.dwclark.btree;

import io.dwclark.btree.io.ImmutableBytes;
import io.dwclark.btree.io.MutableBytes

class BasicNode<K,V> implements Node<K,V> {
    
    int size() {
        throw new UnsupportedOperationException();
    }
    
    public <T extends Stored<K> & Comparable<T>> int key(T locator, V val) {
        throw new UnsupportedOperationException();
    }
    
    boolean leaf(int i) {
        throw new UnsupportedOperationException();
    }
    
    public <T extends Stored<K> & Comparable<T>> void put(T forStorage, Stored<V> val) {
        throw new UnsupportedOperationException();
    }
    
    V get(int i) {
        throw new UnsupportedOperationException();
    }
}

class BasicBTree<K,V> implements BTree<K,V> {

    public <T extends Stored<K> & Comparable<T>> Search<K,V> search(T locator) {
        throw new UnsupportedOperationException();
    }
    
    public <T extends Stored<K> & Comparable<T>> void insert(T locator, Stored<V> val) {
        throw new UnsupportedOperationException();
    }
    
    public <T extends Stored<K> & Comparable<T>> void remove(T locator) {
        throw new UnsupportedOperationException();
    }
}
