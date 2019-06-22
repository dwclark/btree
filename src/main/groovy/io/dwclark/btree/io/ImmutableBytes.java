package io.dwclark.btree.io;

import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;

public interface ImmutableBytes {
    
    static final byte TRUE = (byte) 1;
    static final byte FALSE = (byte) 0;

    //read methods
    byte read(long at);
    byte[] read(long at, byte[] dst, int off, int length);
    short readShort(long at);
    char readChar(long at);
    int readInt(long at);
    long readLong(long at);
    float readFloat(long at);
    double readDouble(long at);

    default byte[] read(final long at, final byte[] dst) {
        return read(at, dst, 0, dst.length);
    }

    default byte[] read(final long at, final int length) {
        return read(at, new byte[length], 0, length);
    }

    default boolean readBoolean(final long at) {
        return read(at) == FALSE ? false : true;
    }

    default String readString(final long at) {
        return readString(at, StandardCharsets.UTF_8);
    }

    default String readString(final long at, final Charset cs) {
        return StringBytes.decode(at, cs, this);
    }

    default int stringLength(final long at) {
        return readInt(at);
    }

    void stop();
}
