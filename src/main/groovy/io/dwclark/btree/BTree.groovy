package io.dwclark.btree

import io.dwclark.btree.io.ImmutableBytes
import io.dwclark.btree.io.MutableBytes

interface Stored<T> {
    int length()
    void toBuffer(int at, MutableBytes buf)
    Stored<T> forScan(int length, int at, ImmutableBytes buf)
    Stored<T> forSave()
    T get()
}

interface Node<K,V> {
    int size()
    public <T extends Stored<K> & Comparable<T>> int key(T locator, V val)
    boolean leaf(int i)
    public <T extends Stored<K> & Comparable<T>> void put(T forStorage, Stored<V> val)
    V get(int i)
}

interface Search<K,V> {
    int getIndex()
    Node<K,V> getNode()
}

interface BTree<K,V> {
    public <T extends Stored<K> & Comparable<T>> Search<K,V> search(T locator)
    public <T extends Stored<K> & Comparable<T>> void insert(T locator, Stored<V> val)
    public <T extends Stored<K> & Comparable<T>> void remove(T locator)
}
