package io.dwclark.btree.io;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.TimeUnit;

public class FalseLock implements Lock {

    private static final FalseLock _instance = new FalseLock();
    public static Lock instance() { return _instance; }

    private static final ReadWriteLock _rwLock = new ReadWriteLock() {
            public Lock readLock() { return _instance; }
            public Lock writeLock() { return _instance; }
        };
    public static ReadWriteLock rwLock() { return _rwLock; }
    
    public void lock() { }
    public void	lockInterruptibly() { }
    public Condition newCondition() { return null; }
    public boolean tryLock() { return true; }
    public boolean tryLock(long time, TimeUnit unit) { return true; }
    public void unlock() { }
}
