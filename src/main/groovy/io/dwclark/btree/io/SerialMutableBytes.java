package io.dwclark.btree.io;

import java.nio.charset.Charset;

public class SerialMutableBytes {

    private final MutableBytes bytes;
    private long readAt;
    private long writeAt;
    
    public SerialMutableBytes(final MutableBytes bytes){
        this(bytes, 0L, 0L);
    }

    public SerialMutableBytes(final MutableBytes bytes, final long readAt, final long writeAt) {
        this.bytes = bytes;
        this.readAt = readAt;
        this.writeAt = writeAt;
    }

    public long getReadAt() {
        return readAt;
    }

    public long getWriteAt() {
        return writeAt;
    }

    public void setReadAt(final long val) {
        readAt = val;
    }

    public void setWriteAt(final long val) {
        writeAt = val;
    }

    public byte read() {
        final byte ret = bytes.read(readAt);
        ++readAt;
        return ret;
    }
            
    public byte[] read(byte[] dst, int off, int length) {
        final byte ret[] = bytes.read(readAt, dst, off, length);
        readAt += length;
        return ret;
    }
    
    public short readShort() {
        final short ret = bytes.readShort(readAt);
        readAt += 2;
        return ret;
    }
    
    public char readChar() {
        final char ret = bytes.readChar(readAt);
        readAt += 2;
        return ret;
    }
    
    public int readInt() {
        final int ret = bytes.readInt(readAt);
        readAt += 4;
        return ret;
    }
    
    public long readLong() {
        final long ret = bytes.readLong(readAt);
        readAt += 8;
        return ret;
    }
    
    public float readFloat() {
        final float ret = bytes.readFloat(readAt);
        readAt += 4;
        return ret;
    }
    
    public double readDouble() {
        final double ret = bytes.readDouble(readAt);
        readAt += 8;
        return ret;
    }

    public byte[] read(final byte[] dst) {
        return read(dst, 0, dst.length);
    }

    public byte[] read(final int length) {
        return read(new byte[length]);
    }

    public boolean readBoolean() {
        final boolean ret = bytes.readBoolean(readAt);
        ++readAt;
        return ret;
    }

    public String readString() {
        final String ret = bytes.readString(readAt);
        readAt += (4 + bytes.stringLength(readAt));
        return ret;
    }

    public String readString(final Charset cs) {
        final String ret = bytes.readString(readAt, cs);
        readAt += (4 + bytes.stringLength(readAt));
        return ret;
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
