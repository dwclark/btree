package io.dwclark.btree.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.BiConsumer;

public class BufferPool {
    private static final long PARALLEL_THRESHOLD = 4L;
    private static final Set<StandardOpenOption> OPTIONS =
        EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    
    private final ConcurrentHashMap<Key,Value> buffers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Object,PathChannel> channels = new ConcurrentHashMap<>();

    class PathChannel {
        final Path path;
        final FileChannel channel;

        public PathChannel(final Path path, final FileChannel channel) {
            this.path = path;
            this.channel = channel;
        }

        public long size() {
            try {
                return channel.size();
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        public Value readFully(final Key key, final Value value) {
            try {
                final ByteBuffer buf = value.buffer;
                final long base = key.getBase();
                buf.limit(buf.capacity());
                buf.position(0);
                int read = 0;
                while(read < buf.capacity()) {
                    read += channel.read(buf, base + read);
                }

                buf.flip();
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
            
            return value;
        }

        public Value writeFully(final Key key, final Value value) {
            try {
                final ByteBuffer buf = value.buffer;
                final long base = key.getBase();
                buf.limit(buf.capacity());
                buf.position(0);
                int written = 0;
                while(written < buf.capacity()) {
                    written += channel.write(buf, base + written);
                }

                buf.flip();
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }

            value.setDirty(false);
            return value;
        }

        public void force() {
            try {
                channel.force(false);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void close() {
            try {
                channel.close();
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    static abstract class Key {
        abstract long getBase();
        abstract Object getId();
        
        @Override
        final public int hashCode() {
            return 37 * Long.hashCode(getBase()) + getId().hashCode();
        }
            
        @Override
        final public boolean equals(final Object o) {
            final Key rhs = (Key) o;
            return (getBase() == rhs.getBase()) && getId().equals(rhs.getId());
        }

        static class Mutable extends Key {
            private long _base;
            private Object _id;

            Mutable setBase(final long val) { _base = val; return this; }
            long getBase() { return _base; }

            Mutable setId(final Object val) { _id = val; return this; }
            Object getId() { return _id; }
        }

        static class Immutable extends Key {
            private final long _base;
            private final Object _id;
                
            public long getBase() { return _base; }
            public Object getId() { return _id; }
                
            public Immutable(final Object id, final long base) {
                _id = id;
                _base = base;
            }
        }

        private static final ThreadLocal<Mutable> _tl = ThreadLocal.withInitial(Mutable::new);

        public static Mutable forSearch(final Object id, final long base) {
            return _tl.get().setId(id).setBase(base);
        }

        public static Immutable forStorage(final Object id, final long base) {
            return new Immutable(id, base);
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
            return at >>> shift;
        }

        protected void set(final Key key, final Value value) {
            currentKey = key;
            currentValue = value;
            currentValue.setLastAccessed(lastAccessed);
        }

        protected Value fill(final Key key, final Value value) {
            final PathChannel pc = channels.get(key.getId());
            
            if(key.getBase() + bufferSize > pc.size()) {
                return handleExpansion(key, value);
            }
            else {
                return pc.readFully(key, value);
            }
        }

        private ByteBuffer locate(final long at, final int length) {
            final long base = base(at);
            if(currentKey.getBase() == base) {
                return currentValue.buffer;
            }

            final Key newKey = Key.forStorage(currentKey.getId(), base);
            final Value loadedValue = buffers.get(newKey);
            if(loadedValue != null) {
                set(newKey, loadedValue);
                return loadedValue.buffer;
            }
            
            Value newValue = new Value(ByteBuffer.allocate(bufferSize));
            newValue = buffers.putIfAbsent(newKey, fill(newKey, newValue));
            set(newKey, newValue);
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

    private final int bufferSize;
    private final int maxBuffers;
    private final long shift;
    private final long mask;
    private final ReadWriteLock rwLock;
    
    public BufferPool(final int bufferSize, final int maxBuffers, final boolean threadSafe) {
        if(!Locator.powerOfTwo(bufferSize)) {
            throw new IllegalArgumentException("buffer size must be a power of two");
        }
        
        this.bufferSize = bufferSize;
        this.maxBuffers = maxBuffers;
        this.shift = Locator.shift(bufferSize);
        this.mask = Locator.mask(bufferSize);
        this.rwLock = threadSafe ? new ReentrantReadWriteLock() : FalseLock.rwLock();
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
    
    public void createChannel(final Object id, final Path path) {
        channels.compute(id, (existingId, existingValue) -> {
                try {
                    return (existingValue == null ?
                            new PathChannel(path, FileChannel.open(path, OPTIONS)) :
                            existingValue);
                }
                catch(IOException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    private void flush(final Key key, final Value value) {
        if(value.isDirty()) {
            channels.get(key.getId()).writeFully(key, value);
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

    public MutableBytes forWrite(final Object id) {
        rwLock.writeLock().lock();

        final Key key = Key.forStorage(id, -1L);
        return new VectorIndexed.Mutable(new ForWrite(key)) {
            public void stop() {
                rwLock.writeLock().unlock();
            }
        };
        
    }

    public ImmutableBytes forRead(final Object id) {
        rwLock.readLock().lock();
        
        final Key key = Key.forStorage(id, -1L);
        return new VectorIndexed.Mutable(new ForRead(key)) {
            public void stop() {
                rwLock.readLock().unlock();
            }
        };
    }
}
