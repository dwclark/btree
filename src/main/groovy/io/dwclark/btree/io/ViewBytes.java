package io.dwclark.btree.io;

import java.util.function.Consumer;
import java.util.function.Function;

public interface ViewBytes {
    ImmutableBytes forRead();
    MutableBytes forWrite();

    default void withRead(final Consumer<ImmutableBytes> consumer) {
        final ImmutableBytes bytes = forRead();
        try {
            consumer.accept(bytes);
        }
        finally {
            bytes.stop();
        }
    }

    default <T> T withRead(final Function<ImmutableBytes,T> func) {
        final ImmutableBytes bytes = forRead();
        try {
            return func.apply(bytes);
        }
        finally {
            bytes.stop();
        }
    }

    default void withWrite(final Consumer<MutableBytes> consumer) {
        final MutableBytes bytes = forWrite();
        try {
            consumer.accept(bytes);
        }
        finally {
            bytes.stop();
        }
    }

    default <T> T withWrite(final Function<MutableBytes,T> func) {
        final MutableBytes bytes = forWrite();
        try {
            return func.apply(bytes);
        }
        finally {
            bytes.stop();
        }
    }
}
