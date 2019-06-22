package io.dwclark.btree.io;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GrowableBuffers implements ViewBytes {
    
    private final List<ByteBuffer> buffers;
    private final int bufferSize;
    private final long shift;
    private final long mask;
    private final Lock readLock;
    private final Lock writeLock;
    private final Loc locator;
        
    public GrowableBuffers(final int bufferSize, final boolean threadSafe) {
        if(bufferSize < 2) {
            throw new IllegalArgumentException("requested buffer size is too small, must be >= 2");
        }

        if(Integer.bitCount(bufferSize) != 1) {
            throw new IllegalArgumentException("requested buffer size must be a power of 2");
        }

        this.bufferSize = bufferSize;
        this.buffers = new ArrayList<>();
        this.shift = Locator.shift(bufferSize);
        this.mask = Locator.mask(bufferSize);
        
        if(threadSafe) {
            final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
            this.readLock = rwLock.readLock();
            this.writeLock = rwLock.writeLock();
        }
        else {
            this.readLock = FalseLock.instance();
            this.writeLock = FalseLock.instance();
        }

        this.locator = new Loc();
    }

    private class Loc implements Locator {
        private void fill(final int index) {
            for(int i = buffers.size(); i <= index; ++i) {
                buffers.add(ByteBuffer.allocate(bufferSize));
            }
        }
        
        private ByteBuffer locate(final long at, final int length) {
            final int index = (int) (at >>> shift);
            if(index <= buffers.size()) {
                fill(index);
            }
            
            return buffers.get(index);
        }

        public ByteBuffer forRead(final long at, final int length) {
            return locate(at, length);
        }

        public ByteBuffer forWrite(final long at, final int length) {
            return locate(at, length);
        }
        
        public int index(final long at) {
            return (int) (mask & at);
        }
    }

    public ImmutableBytes forRead() {
        readLock.lock();

        return new VectorIndexed.Immutable(locator) {
            public void stop() {
                readLock.unlock();
            }
        };
        
    }
    
    public MutableBytes forWrite() {
        writeLock.lock();

        return new VectorIndexed.Mutable(locator) {
            public void stop() {
                writeLock.unlock();
            }
        };
    }
}
