package io.dwclark.btree.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Path;
import java.util.concurrent.locks.ReadWriteLock;

public class PathChannel {
    final Path path;
    final FileChannel channel;
    final ReadWriteLock rwLock;

    public PathChannel(final Path path, final FileChannel channel, final ReadWriteLock rwLock) {
        this.path = path;
        this.channel = channel;
        this.rwLock = rwLock;
    }

    public long size() {
        try {
            return channel.size();
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void readFully(final long base, final ByteBuffer buf) {
        try {
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
    }

    public void writeFully(final long base, final ByteBuffer buf) {
        try {
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
