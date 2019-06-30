package io.dwclark.btree;

import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import io.dwclark.btree.io.FalseLock;
import java.util.function.LongSupplier;
import java.util.function.Function;

public class BlockAllocator {

    private final long max;
    private final Lock lock;
    
    private long _current;
    private final NavigableSet<Long> _unused;
        
    public BlockAllocator(final long max, final boolean threadSafe) {
        this(max, 0L, new TreeSet<>(), threadSafe);
    }

    public BlockAllocator(final long max, final long current,
                          final NavigableSet<Long> unused, final boolean threadSafe) {
        this.max = max;
        this.lock = threadSafe ? new ReentrantLock() : FalseLock.instance();
        _current = current;
        _unused = unused;
    }

    private long withLock(final LongSupplier supplier) {
        lock.lock();
        try {
            return supplier.getAsLong();
        }
        finally {
            lock.unlock();
        }
    }

    private NavigableSet<Long> withUnused(final Function<NavigableSet<Long>,NavigableSet<Long>> func) {
        lock.lock();
        try {
            return func.apply(_unused);
        }
        finally {
            lock.unlock();
        }
    }

    public long getMax() {
        return max;
    }

    public long getCurrent() {
        return withLock(() -> _current);
    }

    public NavigableSet<Long> getUnused() {
        return withUnused((u) -> new TreeSet<>(u));
    }

    public void unused(final long block) {
        if(block < 0L || block >= _current) {
            throw new IllegalArgumentException("illegal block returned");
        }

        withUnused((u) -> { u.add(block); return u; });
    }
    
    public long next() {
        return withLock(() -> {
                final Long val = _unused.pollFirst();
                if(val != null) {
                    return val.longValue();
                }

                if(max == _current) {
                    throw new IllegalStateException("block allocator is exhausted");
                }
                
                return _current++;
            });
    }
}
