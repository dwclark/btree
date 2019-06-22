package io.dwclark.btree.io;

import java.nio.charset.Charset;

public class SerialMutableBytes extends SerialImmutableBytes {

    private final MutableBytes bytes;
    private long writeAt;
    
    public SerialMutableBytes(final MutableBytes bytes) {
        this(bytes, 0L, 0L);
    }
    
    public SerialMutableBytes(final MutableBytes bytes, final long readAt, final long writeAt) {
        super(bytes, readAt);
        this.bytes = bytes;
        this.writeAt = writeAt;
    }

    public long getWriteAt() {
        return writeAt;
    }

    public void setWriteAt(final long val) {
        writeAt = val;
    }

    public SerialMutableBytes write(final byte val) {
        bytes.write(writeAt, val);
        ++writeAt;
        return this;
    }
    
    public SerialMutableBytes write(final byte[] src, final int off, final int length) {
        bytes.write(writeAt, src, off, length);
        writeAt += length;
        return this;
    }
    
    public SerialMutableBytes writeShort(final short val) {
        bytes.writeShort(writeAt, val);
        writeAt += 2;
        return this;
    }
    
    public SerialMutableBytes writeChar(final char val) {
        bytes.writeChar(writeAt, val);
        writeAt += 2;
        return this;
    }
    
    public SerialMutableBytes writeInt(final int val) {
        bytes.writeInt(writeAt, val);
        writeAt += 4;
        return this;
    }
    
    public SerialMutableBytes writeLong(final long val) {
        bytes.writeLong(writeAt, val);
        writeAt += 8;
        return this;
    }
    
    public SerialMutableBytes writeFloat(final float val) {
        bytes.writeFloat(writeAt, val);
        writeAt += 4;
        return this;
    }
    
    public SerialMutableBytes writeDouble(final double val) {
        bytes.writeDouble(writeAt, val);
        writeAt += 8;
        return this;
    }
    
    public SerialMutableBytes write(final byte[] src) {
        return write(src, 0, src.length);
    }

    public SerialMutableBytes writeBoolean(final boolean val) {
        bytes.writeBoolean(writeAt, val);
        ++writeAt;
        return this;
    }
    
    public SerialMutableBytes writeString(final String str) {
        bytes.writeString(writeAt, str);
        writeAt += (4 + bytes.stringLength(writeAt));
        return this;
    }

    public SerialMutableBytes writeString(final String str, final Charset cs) {
        bytes.writeString(writeAt, str, cs);
        writeAt += (4 + bytes.stringLength(writeAt));
        return this;
    }

    public void stop() {
        bytes.stop();
    }
}
