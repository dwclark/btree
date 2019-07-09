package io.dwclark.btree;

import io.dwclark.btree.io.ImmutableBytes;
import io.dwclark.btree.io.MutableBytes;

public class LongRecord implements Record<Long> {

    private static LongRecord _instance = new LongRecord();
    
    public static LongRecord instance() {
        return _instance;
    }
    
    private LongRecord() {}
    
    public int size() { return 8; }

    public Long extract(final ImmutableBytes bytes, final long pos) {
        return Long.valueOf(bytes.readLong(pos));
    }
    
    public void place(final MutableBytes bytes, final long pos, final Long val) {
        bytes.writeLong(pos, val.longValue());
    }
    
    public int compare(final ImmutableBytes bytes, final long pos, final Long lhs) {
        return Long.compare(lhs.longValue(), bytes.readLong(pos));
    }
    
    public int compareInPlace(final ImmutableBytes bytes, final long pos1, final long pos2) {
        return Long.compare(bytes.readLong(pos1), bytes.readLong(pos2));
    }
}
