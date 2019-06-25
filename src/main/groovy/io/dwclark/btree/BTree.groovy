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

interface BTree<K,V> {
    public <T extends Stored<K> & Comparable<T>> Stored<V> search(T locator)
    public <T extends Stored<K> & Comparable<T>> void insert(T locator, Stored<V> val)
    public <T extends Stored<K> & Comparable<T>> void remove(T locator)
}
