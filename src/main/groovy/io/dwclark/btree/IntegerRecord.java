package io.dwclark.btree;

import io.dwclark.btree.io.ImmutableBytes;
import io.dwclark.btree.io.MutableBytes;

public class IntegerRecord implements Record<Integer> {

    private static IntegerRecord _instance = new IntegerRecord();
    
    public static IntegerRecord instance() {
        return _instance;
    }
    
    private IntegerRecord() {}
    
    public int size() { return 4; }

    public Integer extract(final ImmutableBytes bytes, final long pos) {
        return Integer.valueOf(bytes.readInt(pos));
    }
    
    public void place(final MutableBytes bytes, final long pos, final Integer val) {
        bytes.writeInt(pos, val.intValue());
    }
    
    public int compare(final ImmutableBytes bytes, final long pos, final Integer lhs) {
        return Integer.compare(lhs.intValue(), bytes.readInt(pos));
    }
}
