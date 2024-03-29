package io.dwclark.btree;

import io.dwclark.btree.io.ImmutableBytes;
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

    public static class Immutable<K,V> {

        public final int keySize() {
            return keyRecord.size();
        }

        public final int valueSize() {
            return valueRecord.size();
        }

        public final int entrySize() {
            return Node.entrySize(keyRecord, valueRecord);
        }

        public final Record<K> keyRecord;
        public final Record<V> valueRecord;
        final protected ImmutableBytes bytes;
        final protected long base;
        final protected int bufferSize;
        final protected int node;

        protected long pos;
        protected int index;
        protected int count;
        protected boolean leaf;

        public Immutable(final Immutable rhs) {
            this.bytes = rhs.bytes;
            this.keyRecord = rhs.keyRecord;
            this.valueRecord = rhs.valueRecord;
            this.base = rhs.base;
            this.bufferSize = rhs.bufferSize;
            this.node = rhs.node;
            this.pos = rhs.pos;
            this.index = rhs.index;
            this.count = rhs.count;
            this.leaf = rhs.leaf;
        }
        
        public Immutable(final ImmutableBytes bytes, final Record<K> keyRecord, final Record<V> valueRecord,
                         final int node, final int bufferSize) {
            this.bytes = bytes;
            this.keyRecord = keyRecord;
            this.valueRecord = valueRecord;
            this.node = node;
            this.bufferSize = bufferSize;
            
            this.base = ((long) (0xFFFF_FFFFL & node)) * ((long) bufferSize);
            this.count = bytes.readShort(base) & 0x7FFF;
            this.leaf = 0 != (bytes.readShort(base) & 0x8000);
            this.index = 0;
            this.pos = base + COUNT_SIZE;
        }

        public Immutable copy() {
            return new Immutable(this);
        }

        public int node() {
            return node;
        }

        public Immutable<K,V> childNode() {
            return new Immutable<>(bytes, keyRecord, valueRecord, child(), bufferSize);
        }

        public Immutable<K,V> rightChildNode() {
            if(index == count) {
                return null;
            }
            else {
                return new Immutable<>(bytes, keyRecord, valueRecord, rightChild(), bufferSize);
            }
        }

        public Immutable<K,V> leftChildNode() {
            return new Immutable<>(bytes, keyRecord, valueRecord, leftChild(), bufferSize);
        }

        public Immutable<K,V> index(final int index) {
            this.index = index;
            this.pos = base + COUNT_SIZE + (index * entrySize());
            return this;
        }

        public Immutable<K,V> incrementIndex() {
            index += 1;
            pos += entrySize();
            return this;
        }

        public Immutable<K,V> decrementIndex() {
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

        public int childAtIndex(final int idx) {
            final long pos = COUNT_SIZE + (idx * entrySize());
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

        public K key() {
            return keyRecord.extract(bytes, keyPos());
        }

        public long keyPos(final int idx) {
            return base + COUNT_SIZE + CHILD_SIZE + (idx * entrySize());
        }
        
        public K keyAtIndex(final int idx) {
            return keyRecord.extract(bytes, keyPos(idx));
        }

        public long valuePos() {
            return keyPos() + keyRecord.size();
        }

        public V value() {
            return valueRecord.extract(bytes, valuePos());
        }

        public long valuePos(final int idx) {
            return base + COUNT_SIZE + CHILD_SIZE + keySize() + (idx * entrySize());
        }

        public V valueAtIndex(final int idx) {
            return valueRecord.extract(bytes, valuePos(idx));
        }

        public int compareKeyAt(final K k, final int idx) {
            return keyRecord.compare(bytes, keyPos(idx), k);
        }

        public boolean find(final K k) {
            for(; index < count && keyRecord.compare(bytes, keyPos(), k) > 0; ++index) {
                pos += entrySize();
            }
            
            return (index < count) && (keyRecord.compare(bytes, keyPos(), k) == 0);
        }

        private boolean _isSorted() {
            for(index(0); index < (count - 1); incrementIndex()) {
                final long firstPos = pos() + CHILD_SIZE;
                final long secondPos = firstPos + entrySize();
                if(keyRecord.compareInPlace(bytes, firstPos, secondPos) > 0) {
                    final K first = key();
                    final K second = incrementIndex().key();
                    return false;
                }
            }

            return true;
        }
        
        public boolean isSorted() {
            return copy()._isSorted();
        }

        private List<K> _keys() {
            final List<K> ret = new ArrayList<>();
            for(index(0); index < count; incrementIndex()) {
                ret.add(key());
            }
            
            return ret;
        }
        
        public List<K> keys() {
            return copy()._keys();
        }

        private List<V> _values() {
            final List<V> ret = new ArrayList<>();
            for(index(0); index < count; incrementIndex()) {
                ret.add(value());
            }

            return ret;
        }
        
        public List<V> values() {
            return copy()._values();
        }

        public long pos() {
            return pos;
        }

        public ImmutableBytes bytes() {
            return bytes;
        }

        private String _toString() {
            final StringBuilder sb = new StringBuilder();
            
            for(index(0); index < count(); incrementIndex()) {
                sb.append("{");
                if(!leaf()) {
                    sb.append(child()).append(",");
                }
                
                sb.append(key()).append(",");
                sb.append(value()).append("}");
            }
            
            if(!leaf()) {
                sb.append(" {").append(child()).append("}");
            }
            
            return sb.toString();
        }

        @Override
        public String toString() {
            return copy()._toString();
        }
    }

    public static class Mutable<K,V> extends Immutable<K,V> {
        protected final MutableBytes bytes;

        public Mutable(final Mutable rhs) {
            super(rhs);
            this.bytes = rhs.bytes;
        }
        
        public Mutable(final MutableBytes bytes, final Record<K> keyRecord, final Record<V> valueRecord,
                       final int node, final int bufferSize) {
            super(bytes, keyRecord, valueRecord, node, bufferSize);
            this.bytes = bytes;
        }

        @Override
        public Mutable copy() {
            return new Mutable(this);
        }

        @Override
        public Mutable<K,V> childNode() {
            return new Mutable<>(bytes, keyRecord, valueRecord, child(), bufferSize);
        }

        @Override
        public Mutable<K,V> leftChildNode(){
            return new Mutable<>(bytes, keyRecord, valueRecord, leftChild(), bufferSize);
        }

        @Override
        public MutableBytes bytes() {
            return bytes;
        }

        public Mutable<K,V> leftSiblingNode() {
            if(index == 0) {
                return null;
            }
            else {
                return new Mutable<>(bytes, keyRecord, valueRecord, bytes.readInt(pos - entrySize()), bufferSize);
            }
        }

        @Override
        public Mutable<K,V> rightChildNode() {
            if(index == count()) {
                return null;
            }
            else {
                return new Mutable<>(bytes, keyRecord, valueRecord, rightChild(), bufferSize);
            }
        }

        public Mutable<K,V> leaf(final boolean val) {
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

        public Mutable<K,V> count(final int val) {
            final short toWrite = (short) ((bytes.readShort(base) & 0x8000) | val);
            bytes.writeShort(base, toWrite);
            this.count = val;
            return this;
        }

        public Mutable<K,V> incrementCount() {
            return count(count + 1);
        }

        public Mutable<K,V> decrementCount() {
            return count(count - 1);
        }

        public Mutable<K,V> child(final int val) {
            bytes.writeInt(pos, val);
            return this;
        }

        public Mutable<K,V> leftChild(final int val) {
            return child(val);
        }
        
        public Mutable<K,V> rightChild(final int val) {
            bytes.writeInt(pos + entrySize(), val);
            return this;
        }

        public Mutable<K,V> child(final Immutable<K,V> node) {
            return child(node.node);
        }

        public Mutable<K,V> leftChild(final Immutable<K,V> node) {
            return leftChild(node.node);
        }

        public Mutable<K,V> rightChild(final Immutable<K,V> node){
            return rightChild(node.node);
        }

        private int shiftLength() {
            return (entrySize() * (count - index)) + CHILD_SIZE;
        }

        public Mutable<K,V> rightShift() {
            bytes.copy(pos + entrySize(), bytes, pos, shiftLength());
            return this;
        }

        public Mutable<K,V> leftShift() {
            bytes.copy(pos - entrySize(), bytes, pos, shiftLength());
            return this;
        }

        public Mutable<K,V> key(final K k) {
            keyRecord.place(bytes, keyPos(), k);
            return this;
        }

        public Mutable<K,V> value(final V v) {
            valueRecord.place(bytes, valuePos(), v);
            return this;
        }
    }
}
