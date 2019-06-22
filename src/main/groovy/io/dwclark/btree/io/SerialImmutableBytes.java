package io.dwclark.btree.io;

import java.nio.charset.Charset;

public class SerialImmutableBytes {

    private final ImmutableBytes bytes;
    private long readAt;
    
    public SerialImmutableBytes(final ImmutableBytes bytes){
        this(bytes, 0L);
    }

    public SerialImmutableBytes(final ImmutableBytes bytes, final long readAt) {
        this.bytes = bytes;
        this.readAt = readAt;
    }

    public long getReadAt() {
        return readAt;
    }

    public void setReadAt(final long val) {
        readAt = val;
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

    public void stop() {
        bytes.stop();
    }
}
