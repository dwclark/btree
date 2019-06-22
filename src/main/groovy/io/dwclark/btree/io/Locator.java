package io.dwclark.btree.io;

import java.nio.ByteBuffer;

interface Locator {
    ByteBuffer forRead(long at, int length);
    ByteBuffer forWrite(long at, int length);
    int index(long at);

    public static boolean powerOfTwo(final int val) {
        return Integer.highestOneBit(val) == Integer.lowestOneBit(val);
    }
    
    public static int shift(final int bufferSize) {
        return Integer.numberOfTrailingZeros(bufferSize);
    }
    
    public static long mask(final int bufferSize) {
        final int shift = shift(bufferSize);
        long ret = 0L;
        for(long i = 0L; i < shift; ++i) {
            ret |= (1L << i);
        }
        
        return ret;

    }
}
