package io.dwclark.btree.io;

import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;

public interface MutableBytes extends ImmutableBytes {

    MutableBytes write(long at, byte val);
    MutableBytes write(long at, byte[] src, int off, int length);
    MutableBytes writeShort(long at, short val);
    MutableBytes writeChar(long at, char val);
    MutableBytes writeInt(long at, int val);
    MutableBytes writeLong(long at, long val);
    MutableBytes writeFloat(long at, float val);
    MutableBytes writeDouble(long at, double val);
    
    default MutableBytes write(final long at, final byte[] src) {
        return write(at, src, 0, src.length);
    }

    default MutableBytes writeBoolean(final long at, final boolean val) {
        return write(at, val ? TRUE : FALSE);
    }

    default MutableBytes writeString(final long at, final String str) {
        return writeString(at, str, StandardCharsets.UTF_8);
    }

    default MutableBytes writeString(final long at, final String str, final Charset cs) {
        StringBytes.encode(at, str, cs, this);
        return this;
    }
}
