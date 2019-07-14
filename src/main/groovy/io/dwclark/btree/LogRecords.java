package io.dwclark.btree;

import io.dwclark.btree.io.ImmutableBytes;
import io.dwclark.btree.io.MutableBytes;

public abstract class LogRecords {

    public enum LogType {
        START(1), COMMIT(2), ROLLBACK(3), UPDATE(4), INSERT(5), DELETE(6);
        
        private LogType(final short id) {
            this.id = id;
        }

        public final short id;
    }

    public static final StartRecord startInstance = new StartRecord();
    public static final CommitRecord commitInstance = new CommitRecord();
    public static final RollbackRecord rollbackInstance = new RollbackRecord();
    public static final DeleteRecord deleteInstance = new DeleteRecord();

    public static abstract class BaseRecord<T> implements Record<T> {
        public int compare(ImmutableBytes bytes, long pos, T val) {
            throw new UnsupportedOperationException();
        }
        
        public int compareInPlace(ImmutableBytes bytes, long pos1, long pos2) {
            throw new UnsupportedOperationException();
        }
    }

    public interface LogEntry {
        LogType getType();
        long getTxId();
    }

    public abstract static class BaseEntry implements LogEntry {
        private final long txId;

        public BaseEntry(final long txId) {
            this.txId = txId;
        }

        public long getTxId() {
            return txId;
        }
    }

    public static class Start extends BaseEntry {
        public Start(final long txId) {
            super(txId);
        }

        public LogType getType() {
            return LogType.START;
        }
    }

    public static class Commit extends BaseEntry {
        public Commit(final long txId) {
            super(txId);
        }

        public LogType getType() {
            return LogType.COMMIT;
        }
    }

    public static class Rollback extends BaseEntry {
        public Rollback(final long txId) {
            super(txId);
        }

        public LogType getType(){
            return LogType.ROLLBACK;
        }
    }

    public static class Delete extends BaseEntry {
        private final long previous;
        private final int table;
        
        public Delete(final long txId, final long previous, final int table) {
            super(txId);
            this.previous = previous;
            this.table = table;
        }

        public long getPrevious() {
            return prev;
        }

        public int getTable() {
            return table;
        }

        public LogType getType() {
            return LogType.DELETE;
            
        }
    }

    public abstract static class TransactionRecord<T extends BaseEntry> extends BaseRecord<T> {
        public int size() {
            return 8;
        }

        public void place(final MutableBytes bytes, final long pos, final T val) {
            bytes.writeLong(pos, val.getTxId());
        }
    }

    private static final LongRecord lr = LongRecord.instance();
    private static final IntegerRecord ir = IntegerRecord.instance();

    public static class StartRecord extends TransactionRecord<Start> {
        public Start extract(final ImmutableBytes bytes, final long pos) {
            return new Start(bytes.readLong(pos));
        }
    }

    public static class CommitRecord extends TransactionRecord<Commit> {
        public Commit extract(final ImmutableBytes bytes, final long pos) {
            return new Commit(bytes.readLong(pos));
        }
    }

    public static class RollbackRecord extends TransactionRecord<Rollback> {
        public Rollback extract(final ImmutableBytes bytes, final long pos) {
            return new Rollback(bytes.readLong(pos));
        }
    }

    public static class DeleteRecord extends BaseRecord<Delete> {
        public int size() {
            return 20;
        }
        
        public void place(final MutableBytes bytes, final long pos, final Delete val) {
            bytes.writeLong(pos, val.getTxId());
            bytes.writeLong(pos + 8, val.getPrevious());
            bytes.writeInt(pos + 16, val.getTable());
        }
        
        public Delete extract(final ImmutableBytes bytes, final long pos) {
            return new Delete(bytes.readLong(pos),
                              bytes.readLong(pos + 8),
                              bytes.readInt(pos + 16));
        }
    }
}
