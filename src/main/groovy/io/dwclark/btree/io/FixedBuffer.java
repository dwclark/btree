package io.dwclark.btree.io;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FixedBuffer implements ViewBytes {

    private final ByteBuffer buffer;
    private final Lock readLock;
    private final Lock writeLock;

    public FixedBuffer(final int size, final boolean threadSafe) {
        this(ByteBuffer.allocate(size), threadSafe);
    }
    
    public FixedBuffer(final ByteBuffer buffer, final boolean threadSafe) {
        this.buffer = buffer;
        if(threadSafe) {
            final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
            this.readLock = rwLock.readLock();
            this.writeLock = rwLock.writeLock();
        }
        else {
            this.readLock = FalseLock.instance();
            this.writeLock = FalseLock.instance();
        }
    }

    private class Immutable implements ImmutableBytes {
        
        public byte read(final long at) {
            return buffer.get((int) at);
        }
        
        public byte[] read(final long at, final byte[] dst, final int off, final int length) {
            buffer.position((int) at);
            buffer.get(dst, off, length);
            return dst;
        }
        
        public short readShort(final long at) {
            return buffer.getShort((int) at);
        }

        public char readChar(final long at) {
            return buffer.getChar((int) at);
        }

        public int readInt(final long at) {
            return buffer.getInt((int) at);
        }

        public long readLong(final long at) {
            return buffer.getLong((int) at);
        }

        public float readFloat(final long at) {
            return buffer.getFloat((int) at);
        }

        public double readDouble(final long at) {
            return buffer.getDouble((int) at);
        }

        public void stop() {
            readLock.unlock();
        }
    }

    private class Mutable extends Immutable implements MutableBytes {
        
        public Mutable write(long at, byte val) {
            buffer.put((int) at, val);
            return this;
        }
    
        public Mutable write(long at, byte[] src, int off, int length) {
            buffer.position((int) at);
            buffer.put(src, off, length);
            return this;
        }
    
        public Mutable writeShort(long at, short val) {
            buffer.putShort((int) at, val);
            return this;
        }
    
        public Mutable writeChar(long at, char val) {
            buffer.putChar((int) at, val);
            return this;
        }
    
        public Mutable writeInt(long at, int val) {
            buffer.putInt((int) at, val);
            return this;
        }
    
        public Mutable writeLong(long at, long val) {
            buffer.putLong((int) at, val);
            return this;
        }
    
        public Mutable writeFloat(long at, float val) {
            buffer.putFloat((int) at, val);
            return this;
        }
    
        public Mutable writeDouble(long at, double val) {
            buffer.putDouble((int) at, val);
            return this;
        }

        public void stop() {
            writeLock.unlock();
        }
    }

    public ImmutableBytes forRead() {
        readLock.lock();
        return new Immutable();
    }

    public MutableBytes forWrite() {
        writeLock.lock();
        return new Mutable();
    }
}
