package io.dwclark.btree.io;

import java.nio.ByteBuffer;
import java.util.*;

public abstract class VectorIndexed {

    public static abstract class Immutable implements ImmutableBytes {
        protected final Locator locator;

        public Immutable(final Locator locator) {
            this.locator = locator;
        }
    
        public byte read(final long at) {
            return Bridge.read(locator, at);
        }
        
        public byte[] read(long at, byte[] dst, int off, int length) {
            return Bridge.read(locator, at, dst, off, length);
        }
        
        public short readShort(long at) {
            return Bridge.readShort(locator, at);
        }
        
        public char readChar(long at) {
            return Bridge.readChar(locator, at);
        }
        
        public int readInt(long at) {
            return Bridge.readInt(locator, at);
        }
        
        public long readLong(long at) {
            return Bridge.readLong(locator, at);
        }
        
        public float readFloat(long at) {
            return Bridge.readFloat(locator, at);
        }
        
        public double readDouble(long at) {
            return Bridge.readDouble(locator, at);
        }
    }


    public static abstract class Mutable extends Immutable implements MutableBytes {

        public Mutable(final Locator locator) {
            super(locator);
        }
        
        public Mutable write(long at, byte val) {
            Bridge.write(locator, at, val);
            return this;
        }
        
        public Mutable write(long at, byte[] src, int off, int length) {
            Bridge.write(locator, at, src, off, length);
            return this;
        }
        
        public Mutable writeShort(long at, short val) {
            Bridge.writeShort(locator, at, val);
            return this;
        }
        
        public Mutable writeChar(long at, char val) {
            Bridge.writeChar(locator, at, val);
            return this;
        }
        
        public Mutable writeInt(long at, int val) {
            Bridge.writeInt(locator, at, val);
            return this;
        }
        
        public Mutable writeLong(long at, long val) {
            Bridge.writeLong(locator, at, val);
            return this;
        }
        
        public Mutable writeFloat(long at, float val) {
            Bridge.writeFloat(locator, at, val);
            return this;
        }
        
        public Mutable writeDouble(long at, double val) {
            Bridge.writeDouble(locator, at, val);
            return this;
        }
    }
}
