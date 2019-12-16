# BTree Experimental Project

A naive attempt to learn more about databases, particularly i/o and btrees.

## I/O

There is some interesting stuff here with i/o. It's basically a way to try and do uniform i/o for data sizes larger than the a ByteBuffer can address. That's in the Bytes class and associated implementations. It's also an attempt to try and abstract the underlying storage from the Bytes interface.

It works, but after doing more formal study of databases, I don't think it's very useful. Maybe I'll think of another use for it some day.

## BTree

There is a working implementation of a BTree that is pretty generic. It's tested and follows the basic ideas in Cormen, Leiserson, Rivest, and Stein. However, it turned out to be a dead end for the following reasons

* The idea of having a logical and single (but very large) interface to the underlying bytes ends up being counterproductive for b-trees. B-Trees assume writing and reading in blocks, which doesn't match the Bytes interface. This leads to dumb things like converting block oriented addressing in the BTree to a single offset for Bytes...only to then convert that back to block addressing when the underlying storage is block oriented. Since it's all integers it's probably not a big deal in terms of performance, but it is annoying and overly complicated.
* b-trees are more complicated than b+-brees to implement
* b-trees waste more space tha b+-trees (the intermediate nodes have empty bytes)
* b-trees require more disk accesses than b+-trees because the intermediate nodes have fewer keys
* The implementation is slow. Normally I like optimizing this kind of stuff, but the above reasons make optimizing it hard and not interesting.

## Conclusion

If you find anything useful here, feel free to use it whole or in part. But, this is almost certainly dead at this point.
