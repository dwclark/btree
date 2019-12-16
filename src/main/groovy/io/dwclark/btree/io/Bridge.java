package io.dwclark.btree.io;

import java.util.function.Supplier;
import java.nio.ByteBuffer;

class Bridge {

    private static final ThreadLocal<byte[]> _tl = ThreadLocal.withInitial(() -> new byte[8]);
    
    private static byte[] _short(final short val) {
        final byte[] ret = _tl.get();
        ret[0] = (byte) (0xFF & (val >>> 8));
        ret[1] = (byte) (0xFF & val);
        return ret;
    }

    private static short _short(final byte[] ary) {
        int accum = 0;
        accum |= (0xFF & ary[0]) << 8;
        accum |= 0xFF & ary[1];
        return (short) accum;
    }

    private static byte[] _char(final char c) {
        final int val = (int) c;
        final byte[] ret = _tl.get();
        ret[0] = (byte) (0xFF & (val >>> 8));
        ret[1] = (byte) (0xFF & val);
        return ret;
    }

    private static char _char(final byte[] ary) {
        int accum = 0;
        accum |= (0xFF & ary[0]) << 8;
        accum |= (0xFF & ary[1]);
        return (char) accum;
    }

    private static byte[] _int(final int val) {
        final byte[] ret = _tl.get();
        ret[0] = (byte) (0xFF & (val >>> 24));
        ret[1] = (byte) (0xFF & (val >>> 16));
        ret[2] = (byte) (0xFF & (val >>> 8));
        ret[3] = (byte) (0xFF & val);
        return ret;
    }

    private static int _int(final byte[] ary) {
        int accum = 0;
        accum |= (0xFF & ary[0]) << 24;
        accum |= (0xFF & ary[1]) << 16;
        accum |= (0xFF & ary[2]) << 8;
        accum |= (0xFF & ary[3]);
        return accum;
    }

    private static byte[] _long(final long val) {
        final byte[] ret = _tl.get();
        ret[0] = (byte) (0xFF & (val >>> 56L));
        ret[1] = (byte) (0xFF & (val >>> 48L));
        ret[2] = (byte) (0xFF & (val >>> 40L));
        ret[3] = (byte) (0xFF & (val >>> 32L));
        ret[4] = (byte) (0xFF & (val >>> 24L));
        ret[5] = (byte) (0xFF & (val >>> 16L));
        ret[6] = (byte) (0xFF & (val >>> 8L));
        ret[7] = (byte) (0xFF & val);
        return ret;
    }
    
    private static long _long(final byte[] ary) {
        long accum = 0L;
        accum |= (0xFFL & ary[0]) << 56L;
        accum |= (0xFFL & ary[1]) << 48L;
        accum |= (0xFFL & ary[2]) << 40L;
        accum |= (0xFFL & ary[3]) << 32L;
        accum |= (0xFFL & ary[4]) << 24L;
        accum |= (0xFFL & ary[5]) << 16L;
        accum |= (0xFFL & ary[6]) << 8L;
        accum |= (0xFFL & ary[7]);
        return accum;
    }

    private static byte[] _float(final float val) {
        return _int(Float.floatToRawIntBits(val));
    }

    private static float _float(final byte[] ary) {
        return Float.intBitsToFloat(_int(ary));
    }

    private static byte[] _double(final double val) {
        return _long(Double.doubleToRawLongBits(val));
    }

    private static double _double(final byte[] val) {
        return Double.longBitsToDouble(_long(val));
    }

    //TODO: Convert this to use iterative methods, will break in case of large byte[] arrays
    public static int write(final Locator locator, final long at, final byte[] val, final int off, final int length) {
        final ByteBuffer buf = locator.forWrite(at, length);
        final int idx = locator.index(at);
        final int lengthToWrite = Math.min(buf.capacity() - idx, length);
        buf.position(idx);
        buf.put(val, off, lengthToWrite);
        if(lengthToWrite < length) {
            write(locator, at + lengthToWrite, val, off + lengthToWrite, length - lengthToWrite);
        }

        return length;
    }

    public static int write(final Locator locator, final long at, final byte val) {
        final ByteBuffer buf = locator.forWrite(at, 1);
        final int idx = locator.index(at);
        buf.put(idx, val);
        return 1;
    }

    public static int writeShort(final Locator locator, final long at, final short val) {
        final ByteBuffer buf = locator.forWrite(at, 2);
        final int idx = locator.index(at);
        if((idx + 2) <= buf.capacity()) {
            buf.putShort(idx, val);
        }
        else {
            write(locator, at, _short(val), 0, 2);
        }
        
        return 2;
    }

    public static int writeChar(final Locator locator, final long at, final char val) {
        final ByteBuffer buf = locator.forWrite(at, 2);
        final int idx = locator.index(at);
        if((idx + 2) <= buf.capacity()) {
            buf.putChar(idx, val);
        }
        else {
            write(locator, at, _char(val), 0, 2);
        }
        
        return 2;
    }

    
    public static int writeInt(final Locator locator, final long at, final int val) {
        final ByteBuffer buf = locator.forWrite(at, 4);
        final int idx = locator.index(at);
        if((idx + 4) <= buf.capacity()) {
            buf.putInt(idx, val);
        }
        else {
            write(locator, at, _int(val), 0, 4);
        }

        return 4;
    }

    public static int writeLong(final Locator locator, final long at, final long val) {
        final ByteBuffer buf = locator.forWrite(at, 8);
        final int idx = locator.index(at);
        if((idx + 8) <= buf.capacity()) {
            buf.putLong(idx, val);
        }
        else {
            write(locator, at, _long(val), 0, 8);
        }

        return 8;
    }

    public static int writeFloat(final Locator locator, final long at, final float val) {
        final ByteBuffer buf = locator.forWrite(at, 4);
        final int idx = locator.index(at);
        if((idx + 4) <= buf.capacity()) {
            buf.putFloat(idx, val);
        }
        else {
            write(locator, at, _float(val), 0, 4);
        }

        return 4;
    }

    public static int writeDouble(final Locator locator, final long at, final double val) {
        final ByteBuffer buf = locator.forWrite(at, 8);
        final int idx = locator.index(at);
        if((idx + 8) <= buf.capacity()) {
            buf.putDouble(idx, val);
        }
        else {
            write(locator, at, _double(val), 0, 8);
        }

        return 8;
    }

    //TODO: Convert this to use iterative methods, will break in case of long byte[] arrays
    public static byte[] read(final Locator locator, final long at, final byte[] target, final int off, final int length) {
        final ByteBuffer buf = locator.forRead(at, length);
        final int idx = locator.index(at);
        final int lengthToRead = Math.min(buf.capacity() - idx, length);
        buf.position(idx);
        buf.get(target, off, lengthToRead);
        if(lengthToRead < length) {
            read(locator, at + lengthToRead, target, off + lengthToRead, length - lengthToRead);
        }

        return target;
    }

    public static byte read(final Locator locator, final long at) {
        final ByteBuffer buf = locator.forRead(at, 1);
        final int idx = locator.index(at);
        return buf.get(idx);
    }

    public static short readShort(final Locator locator, final long at) {
        final ByteBuffer buf = locator.forRead(at, 2);
        final int idx = locator.index(at);
        final int lengthToRead = Math.min(buf.capacity() - idx, 2);
        if(lengthToRead < 2) {
            return _short(read(locator, at, _tl.get(), 0, 2));
        }
        else {
            return buf.getShort(idx);
        }
    }
    
    public static char readChar(final Locator locator, final long at) {
        final ByteBuffer buf = locator.forRead(at, 2);
        final int idx = locator.index(at);
        final int lengthToRead = Math.min(buf.capacity() - idx, 2);
        if(lengthToRead < 2) {
            return _char(read(locator, at, _tl.get(), 0, 2));
        }
        else {
            return buf.getChar(idx);
        }
    }

    public static int readInt(final Locator locator, final long at) {
        final ByteBuffer buf = locator.forRead(at, 4);
        final int idx = locator.index(at);
        final int lengthToRead = Math.min(buf.capacity() - idx, 4);
        if(lengthToRead < 4) {
            return _int(read(locator, at, _tl.get(), 0, 4));
        }
        else {
            return buf.getInt(idx);
        }
    }

    public static long readLong(final Locator locator, final long at) {
        final ByteBuffer buf = locator.forRead(at, 8);
        final int idx = locator.index(at);
        final int lengthToRead = Math.min(buf.capacity() - idx, 8);
        if(lengthToRead < 8) {
            return _long(read(locator, at, _tl.get(), 0, 8));
        }
        else {
            return buf.getLong(idx);
        }
    }

    public static float readFloat(final Locator locator, final long at) {
        final ByteBuffer buf = locator.forRead(at, 4);
        final int idx = locator.index(at);
        final int lengthToRead = Math.min(buf.capacity() - idx, 4);
        if(lengthToRead < 4) {
            return _float(read(locator, at, _tl.get(), 0, 4));
        }
        else {
            return buf.getFloat(idx);
        }
    }

    public static double readDouble(final Locator locator, final long at) {
        final ByteBuffer buf = locator.forRead(at, 8);
        final int idx = locator.index(at);
        final int lengthToRead = Math.min(buf.capacity() - idx, 8);
        if(lengthToRead < 8) {
            return _double(read(locator, at, _tl.get(), 0, 8));
        }
        else {
            return buf.getDouble(idx);
        }
    }
}
