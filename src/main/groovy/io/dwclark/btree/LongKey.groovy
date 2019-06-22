package io.dwclark.btree

import groovy.transform.CompileStatic
import io.dwclark.btree.io.MutableBytes;
import io.dwclark.btree.io.ImmutableBytes;

@CompileStatic
abstract class LongKey implements Stored<Long>, Comparable<LongKey> {

    static final int SIZE = 8
    
    int length() {
        return SIZE
    }

    Long get() {
        return longValue()
    }

    abstract long longValue()

    @Override
    int compareTo(final LongKey rhs) {
        return Long.compare(longValue(), rhs.longValue())
    }
    
    private static class ForSave extends LongKey {
        final long val

        ForSave(final long val) {
            this.val = val
        }

        void toBuffer(final int at, final MutableBytes mb) {
            mb.writeLong(at, val)
        }

        long longValue() {
            return val
        }

        ForScan forScan(int length, int at, ImmutableBytes buf) {
            return _forScan(length, at, buf)
        }

        ForSave forSave() {
            return this
        }
    }

    private static class ForScan extends LongKey {
        int length
        int at
        ImmutableBytes buf

        void toBuffer(final int at, final MutableBytes buf) {
            throw new UnsupportedOperationException()
        }

        long longValue() {
            return buf.readLong(at)
        }

        ForScan forScan(final int length, final int at, final ImmutableBytes buf) {
            return _forScan(length, at, buf)
        }

        ForSave forSave() {
            return new ForSave(longValue())
        }
    }

    private static final ThreadLocal<ForScan> _tl = new ThreadLocal<ForScan>(){
        @Override
        protected ForScan initialValue() {
            return new ForScan()
        }
    }

    private static ForScan _forScan(final int length, final int at, final ImmutableBytes buf) {
        ForScan ret = _tl.get()
        ret.length = length
        ret.at = at
        ret.buf = buf
        return ret
    }
}
