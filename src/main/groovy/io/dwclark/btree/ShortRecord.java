package io.dwclark.btree;

import io.dwclark.btree.io.ImmutableBytes;
import io.dwclark.btree.io.MutableBytes;

public class ShortRecord implements Record<Short> {

    private static ShortRecord _instance = new ShortRecord();
    
    public static ShortRecord instance() {
        return _instance;
    }
    
    private ShortRecord() {}
    
    public int size() { return 4; }

    public Short extract(final ImmutableBytes bytes, final long pos) {
        return Short.valueOf(bytes.readShort(pos));
    }
    
    public void place(final MutableBytes bytes, final long pos, final Short val) {
        bytes.writeInt(pos, val.shortValue());
    }
    
    public int compare(final ImmutableBytes bytes, final long pos, final Short lhs) {
        return Short.compare(lhs.shortValue(), bytes.readShort(pos));
    }

    public int compareInPlace(final ImmutableBytes bytes, final long pos1, final long pos2) {
        return Short.compare(bytes.readShort(pos1), bytes.readShort(pos2));
    }
}
