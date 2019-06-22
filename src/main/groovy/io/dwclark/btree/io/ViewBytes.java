package io.dwclark.btree.io;

interface ViewBytes {
    ImmutableBytes forRead();
    MutableBytes forWrite();
}
