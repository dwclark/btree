package io.dwclark.btree.io;

public interface ViewBytes {
    ImmutableBytes forRead();
    MutableBytes forWrite();
}
