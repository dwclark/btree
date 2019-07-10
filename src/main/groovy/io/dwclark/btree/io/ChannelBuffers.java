package io.dwclark.btree.io;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

public class ChannelBuffers {

    public enum Locking { NONE, CHANNEL, POOL }
        
    static class Key {
        final Object id;
        final long base;
        
        @Override
        final public int hashCode() {
            return 37 * Long.hashCode(base) + id.hashCode();
        }
            
        @Override
        final public boolean equals(final Object o) {
            final Key rhs = (Key) o;
            return (base == rhs.base) && id.equals(rhs.id);
        }

        public Key(final Object id, final long base) {
            this.id = id;
            this.base = base;
        }

        public static Key make(final Object id, final long base) {
            return new Key(id, base);
        }
    }

    static class Value implements Comparable<Value> {
        private long lastAccessed;
        private boolean dirty;
        final ByteBuffer buffer;
        
        Value(final ByteBuffer buffer) {
            this.buffer = buffer;
            dirty = false;
        }

        void setDirty(final boolean val) { dirty = val; }
        boolean isDirty() { return dirty; }

        long getLastAccessed() { return lastAccessed; }
        void setLastAccessed(final long val) { lastAccessed = val; }

        public int compareTo(final Value rhs) {
            return Long.compare(lastAccessed, rhs.lastAccessed);
        }
    }

    abstract private class BaseLocator implements Locator {
        final long lastAccessed;
        Key currentKey;
        Value currentValue;

        BaseLocator(final Key key) {
            currentKey = key;
            lastAccessed = System.nanoTime();
        }
        
        public int index(long at) {
            return (int) (mask & at);
        }

        protected long base(final long at) {
            return bufferSize * (at >>> shift);
        }

        protected void set(final Key key, final Value value) {
            currentKey = key;
            currentValue = value;
            currentValue.setLastAccessed(lastAccessed);
        }

        protected Value fill(final Key key, final Value value) {
            final PathChannel pc = channels.get(key.id);
            
            if(key.base + bufferSize > pc.size()) {
                return handleExpansion(key, value);
            }
            else {
                pc.readFully(key.base, value.buffer);
                return value;
            }
        }

        private ByteBuffer locate(final long at, final int length) {
            final long base = base(at);
            if(currentKey.base == base) {
                return currentValue.buffer;
            }

            final Key newKey = Key.make(currentKey.id, base);
            final Value loadedValue = buffers.get(newKey);
            if(loadedValue != null) {
                set(newKey, loadedValue);
                return loadedValue.buffer;
            }
            
            final Value newValue = new Value(ByteBuffer.allocate(bufferSize));
            final Value foundValue = buffers.putIfAbsent(newKey, fill(newKey, newValue));
            set(newKey, foundValue == null ? newValue : foundValue);
            return newValue.buffer;
        }

        public ByteBuffer forRead(final long at, final int length) {
            return locate(at, length);
        }

        public ByteBuffer forWrite(final long at, final int length) {
            final ByteBuffer ret = locate(at, length);
            currentValue.setDirty(true);
            return ret;
        }

        abstract protected Value handleExpansion(Key key, Value value);
    }

    private class ForRead extends BaseLocator {
        
        public ForRead(final Key key) {
            super(key);
        }
        
        public Value handleExpansion(final Key key, final Value value) {
            throw new IllegalArgumentException("attempt to read beyond end of file");
        }
    }

    private class ForWrite extends BaseLocator {

        public ForWrite(final Key key) {
            super(key);
        }

        public Value handleExpansion(final Key key, final Value value) {
            return value;
        }
    }

    private static final long PARALLEL_THRESHOLD = 4L;
    private static final Set<StandardOpenOption> OPTIONS =
        EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
    
    private final ConcurrentHashMap<Key,Value> buffers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Object,PathChannel> channels = new ConcurrentHashMap<>();

    private final int bufferSize;
    private final int maxBuffers;
    private final long shift;
    private final long mask;
    private final Locking locking;
    private final ReadWriteLock rwLock;
    
    public ChannelBuffers(final int bufferSize, final int maxBuffers, final Locking locking) {
        if(!Locator.powerOfTwo(bufferSize)) {
            throw new IllegalArgumentException("buffer size must be a power of two");
        }
        
        this.bufferSize = bufferSize;
        this.maxBuffers = maxBuffers;
        this.shift = Locator.shift(bufferSize);
        this.mask = Locator.mask(bufferSize);
        this.locking = locking;
        this.rwLock = (locking == Locking.POOL) ? new ReentrantReadWriteLock() : FalseLock.rwLock();
    }

    public int getBufferCount() {
        return buffers.size();
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public int getMaxBuffers() {
        return maxBuffers;
    }

    public long size(final SizeUnit units) {
        final long tmp = ((long) bufferSize) * ((long) buffers.size());
        return units.convert(tmp, SizeUnit.BYTES);
    }

    public long maxSize(final SizeUnit units) {
        final long tmp = ((long) bufferSize) * ((long) maxBuffers);
        return units.convert(tmp, SizeUnit.BYTES);
    }
    
    public void createChannel(final Object id, final String path) {
        createChannel(id, Paths.get(path));
    }

    public void createChannel(final Object id, final File file) {
        createChannel(id, file.toPath());
    }
    
    public void createChannel(final Object id, final Path path) {
        final ReadWriteLock lock = (locking == Locking.CHANNEL) ? new ReentrantReadWriteLock() : FalseLock.rwLock();
        
        channels.compute(id, (existingId, existingValue) -> {
                try {
                    return (existingValue == null ?
                            new PathChannel(path, FileChannel.open(path, OPTIONS), lock) :
                            existingValue);
                }
                catch(IOException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    private void flush(final Key key, final Value value) {
        if(value.isDirty()) {
            channels.get(key.id).writeFully(key.base, value.buffer);
            value.setDirty(false);
        }
    }

    public void flush() {
        try {
            rwLock.writeLock().lock();
            buffers.forEach(PARALLEL_THRESHOLD, this::flush);
            channels.forEachValue(PARALLEL_THRESHOLD, PathChannel::force);
        }
        finally {
            rwLock.writeLock().unlock();
        }
    }

    public void shutdown() {
        try {
            rwLock.writeLock().lock();
            buffers.forEach(PARALLEL_THRESHOLD, this::flush);
            channels.forEachValue(PARALLEL_THRESHOLD, PathChannel::close);
            buffers.clear();
            channels.clear();
        }
        finally {
            rwLock.writeLock().unlock();
        }
    }

    private ReadWriteLock opLock(final Object id) {
        switch(locking) {
        case NONE: return FalseLock.rwLock();
        case CHANNEL: return channels.get(id).rwLock;
        case POOL: return rwLock;
        default:
            throw new IllegalStateException("unmatched locking enum");
        }
    }

    public MutableBytes forWrite(final Object id) {
        final Lock lock = opLock(id).writeLock();
        lock.lock();

        final Key key = Key.make(id, -1L);
        return new VectorIndexed.Mutable(new ForWrite(key)) {
            public void stop() {
                lock.unlock();
            }
        };
        
    }

    public ImmutableBytes forRead(final Object id) {
        final Lock lock = opLock(id).readLock();
        lock.lock();
        
        final Key key = Key.make(id, -1L);
        return new VectorIndexed.Immutable(new ForRead(key)) {
            public void stop() {
                lock.unlock();
            }
        };
    }

    public ViewBytes viewBytes(final Object id) {
        return new ViewBytes() {
            public ImmutableBytes forRead() {
                return ChannelBuffers.this.forRead(id);
            }

            public MutableBytes forWrite() {
                return ChannelBuffers.this.forWrite(id);
            }
        };
    }
}
