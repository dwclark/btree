package io.dwclark.btree;

import io.dwclark.btree.io.ImmutableBytes;
import io.dwclark.btree.io.MutableBytes;
import io.dwclark.btree.io.ViewBytes;
import io.dwclark.btree.io.MutableBytes;
import io.dwclark.btree.io.ViewBytes;
import java.util.ArrayList;
import java.util.List;

public class Node {

    public static final int COUNT_SIZE = 2;
    public static final int CHILD_SIZE = 4;
    public static final int META_SIZE = COUNT_SIZE + CHILD_SIZE;

    public static <K,V> int entrySize(final Record<K> keyRecord, final Record<V> valueRecord) {
        return CHILD_SIZE + keyRecord.size() + valueRecord.size();
    }

    public static class Immutable<T extends ImmutableBytes> {

        protected static final LongRecord keyRecord = LongRecord.instance();
        protected static final LongRecord valueRecord = LongRecord.instance();

        public final int keySize() {
            return keyRecord.size();
        }

        public final int valueSize() {
            return valueRecord.size();
        }

        public final int entrySize() {
            return Node.entrySize(keyRecord, valueRecord);
        }
        
        protected long pos;
        protected long base;
        protected int bufferSize;
        protected int node;
        protected int index;
        protected int count;
        protected boolean leaf;
        protected T bytes;
        
        public Immutable(final T bytes, final int node, final int bufferSize) {
            this.bufferSize = bufferSize;
            this.bytes = bytes;
            this.node = node;
            this.base = ((long) (0xFFFF_FFFFL & node)) * ((long) bufferSize);
            this.count = bytes.readShort(base) & 0x7FFF;
            this.leaf = 0 != (bytes.readShort(base) & 0x8000);
            this.index = 0;
            this.pos = base + COUNT_SIZE;
        }

        public Immutable childNode() {
            return new Immutable(bytes, child(), bufferSize);
        }

        public Immutable rightChildNode() {
            if(index == count) {
                return null;
            }
            else {
                return new Immutable(bytes, rightChild(), bufferSize);
            }
        }

        public Immutable leftChildNode() {
            return new Immutable(bytes, leftChild(), bufferSize);
        }

        public Immutable index(final int index) {
            this.index = index;
            this.pos = base + COUNT_SIZE + (index * entrySize());
            return this;
        }

        public Immutable incrementIndex() {
            index += 1;
            pos += entrySize();
            return this;
        }

        public Immutable decrementIndex() {
            index -= 1;
            pos -= entrySize();
            return this;
        }

        public int count() {
            return count;
        }

        public boolean leaf() {
            return leaf;
        }

        public int index() {
            return index;
        }

        public int child() {
            return bytes.readInt(pos);
        }

        public int leftChild() {
            return child();
        }

        public int rightChild() {
            return bytes.readInt(pos + entrySize());
        }

        public long keyPos(){
            return pos + CHILD_SIZE;
        }

        public long key() {
            return keyRecord.extract(bytes, keyPos());
        }

        public long valuePos() {
            return keyPos() + keyRecord.size();
        }

        public long value() {
            return valueRecord.extract(bytes, valuePos());
        }

        public boolean find(final long k) {
            for(; index < count && keyRecord.compare(bytes, keyPos(), k) > 0; ++index) {
                pos += entrySize();
            }
            
            return (index < count) && (keyRecord.compare(bytes, keyPos(), k) == 0);
        }

        public List<Long> keys() {
            final List<Long> ret = new ArrayList<>();
            index(0);
            while(index < count) {
                ret.add(key());
                incrementIndex();
            }

            return ret;
        }

        public List<Long> values() {
            final List<Long> ret = new ArrayList<>();
            index(0);
            while(index < count) {
                ret.add(value());
                incrementIndex();
            }

            return ret;
        }

        public long pos() {
            return pos;
        }

        public ImmutableBytes bytes() {
            return bytes;
        }
    }

    public static class Mutable extends Immutable<MutableBytes> {
        Mutable(final MutableBytes bytes, final int node, final int bufferSize) {
            super(bytes, node, bufferSize);
        }

        @Override
        public Mutable childNode() {
            return new Mutable(bytes, child(), bufferSize);
        }

        @Override
        public Mutable leftChildNode(){
            return new Mutable(bytes, leftChild(), bufferSize);
        }

        @Override
        public MutableBytes bytes() {
            return bytes;
        }

        public Mutable leftSiblingNode() {
            if(index == 0) {
                return null;
            }
            else {
                return new Mutable(bytes, bytes.readInt(pos - entrySize()), bufferSize);
            }
        }

        @Override
        public Mutable rightChildNode() {
            if(index == count()) {
                return null;
            }
            else {
                return new Mutable(bytes, rightChild(), bufferSize);
            }
        }

        public Mutable leaf(final boolean val) {
            final short now = bytes.readShort(base);
            if(val) {
                bytes.writeShort(base, (short) (now | 0x8000));
            }
            else {
                bytes.writeShort(base, (short) (now & 0x7FFF));
            }

            this.leaf = val;
            return this;
        }

        public Mutable count(final int val) {
            final short toWrite = (short) ((bytes.readShort(base) & 0x8000) | val);
            bytes.writeShort(base, toWrite);
            this.count = val;
            return this;
        }

        public Mutable incrementCount() {
            return count(count + 1);
        }

        public Mutable decrementCount() {
            return count(count - 1);
        }

        public Mutable child(final int val) {
            bytes.writeInt(pos, val);
            return this;
        }

        public Mutable leftChild(final int val) {
            return child(val);
        }
        
        public Mutable rightChild(final int val) {
            bytes.writeInt(pos + entrySize(), val);
            return this;
        }

        public Mutable child(final Immutable node) {
            return child(node.node);
        }

        public Mutable leftChild(final Immutable node) {
            return leftChild(node.node);
        }

        public Mutable rightChild(final Immutable node){
            return rightChild(node.node);
        }

        private int shiftLength() {
            return (entrySize() * (count - index)) + CHILD_SIZE;
        }

        public Mutable rightShift() {
            bytes.copy(pos + entrySize(), bytes, pos, shiftLength());
            return this;
        }

        public Mutable leftShift() {
            bytes.copy(pos - entrySize(), bytes, pos, shiftLength());
            return this;
        }

        public Mutable key(final long k) {
            bytes.writeLong(pos + CHILD_SIZE, k);
            return this;
        }

        public Mutable value(final long v) {
            bytes.writeLong(pos + CHILD_SIZE + keyRecord.size(), v);
            return this;
        }
    }
}
