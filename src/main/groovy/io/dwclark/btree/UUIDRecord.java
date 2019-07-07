package io.dwclark.btree;

import io.dwclark.btree.io.ImmutableBytes;
import io.dwclark.btree.io.MutableBytes;
import java.util.UUID;

public class UUIDRecord implements Record<UUID> {

    private static final long MASK = 0xFFFF_FFFFL;
    private static UUIDRecord _instance = new UUIDRecord();
    
    public static UUIDRecord instance() {
        return _instance;
    }
    
    private UUIDRecord() {}
    
    public int size() { return 16; }

    public UUID extract(final ImmutableBytes bytes, final long pos) {
        return new UUID(bytes.readLong(pos), bytes.readLong(pos + 8));
    }
    
    public void place(final MutableBytes bytes, final long pos, final UUID val) {
        bytes.writeLong(pos, val.getMostSignificantBits());
        bytes.writeLong(pos + 8, val.getLeastSignificantBits());
    }
    
    public int compare(final ImmutableBytes bytes, final long pos, final UUID lhs) {
        long _1 = lhs.getMostSignificantBits();
        long _2 = bytes.readLong(pos);

        if(_1 < _2) return -1;
        if(_1 > _2) return 1;

        _1 = lhs.getLeastSignificantBits();
        _2 = bytes.readLong(pos + 8);

        if(_1 < _2) return -1;
        if(_1 > _2) return 1;

        return 0;
    }
}
