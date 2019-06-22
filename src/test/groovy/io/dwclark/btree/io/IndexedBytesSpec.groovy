package io.dwclark.btree.io

import spock.lang.*
import static java.nio.charset.StandardCharsets.UTF_8

class IndexedBytesSpec extends Specification {

    def "Basic Single Buffer Operations"() {
        setup:
        
        def str = "this is a stupid string"
        def fb = new FixedBuffer(1_024, false);
        def sb = new SerialMutableBytes(fb.forWrite())
        sb.with {
            write 1 as byte
            writeShort 2 as short
            writeInt 137
            writeLong Long.MAX_VALUE
            writeFloat 5f
            writeDouble 10d
            writeString str, UTF_8
        }
        
        expect:

        sb.with {
            assert read() == 1
            assert readShort() == 2 as short
            assert readInt() == 137
            assert readLong() == Long.MAX_VALUE
            assert readFloat() == 5f
            assert readDouble() == 10d
            assert readString(UTF_8) == str
        }
    }

    def "short vectored operations"() {
        setup:

        def grow = new GrowableBuffers(4, false);
        def sb = new SerialMutableBytes(grow.forWrite());
        sb.with {
            writeShort 100 as short
            write 1 as byte
            writeShort 100 as short
        }

        expect:

        sb.with {
            assert readShort() == 100 as short
            assert read() == 1
            assert readShort() == 100 as short
        }
    }

    def "char vectored operations"() {
        setup:

        def grow = new GrowableBuffers(4, false)
        def sb = new SerialMutableBytes(grow.forWrite())
        sb.with {
            writeChar 'A' as char
            write 1 as byte
            writeChar 'A' as char
        }

        expect:

        sb.with {
            assert readChar() == 'A' as char
            assert read() == 1
            assert readChar() == 'A' as char
        }
    }


    def "int vectored operations"() {
        setup:

        def grow = new GrowableBuffers(4, false)
        def sb = new SerialMutableBytes(grow.forWrite())
        sb.with {
            writeInt Integer.MAX_VALUE
            write 1 as byte
            writeInt Integer.MAX_VALUE
        }

        expect:

        sb.with {
            assert readInt() == Integer.MAX_VALUE
            assert read() == 1
            assert readInt() == Integer.MAX_VALUE
        }
    }

    def "long vectored operations"() {
        setup:

        def grow = new GrowableBuffers(8, false)
        def sb = new SerialMutableBytes(grow.forWrite())
        sb.with {
            writeLong Long.MAX_VALUE
            write 1 as byte
            write 1 as byte
            write 2 as byte
            writeLong Long.MAX_VALUE
        }

        expect:

        sb.with {
            assert readLong() == Long.MAX_VALUE
            assert read() == 1 as byte
            assert read() == 1 as byte
            assert read() == 2 as byte
            assert readLong() == Long.MAX_VALUE
        }
    }

    def "float vectored operations"() {
        setup:

        def grow = new GrowableBuffers(4, false)
        def sb = new SerialMutableBytes(grow.forWrite())
        sb.with {
            writeFloat Float.MAX_VALUE
            write 1 as byte
            writeFloat Float.MAX_VALUE
        }

        expect:

        sb.with {
            assert readFloat() == Float.MAX_VALUE
            assert read() == 1
            assert readFloat() == Float.MAX_VALUE
        }   
    }

    def "double vectored operations"() {
        setup:

        def grow = new GrowableBuffers(8, false)
        def sb = new SerialMutableBytes(grow.forWrite())
        sb.with {
            writeDouble Double.MAX_VALUE
            write 1 as byte
            write 1 as byte
            write 2 as byte
            writeDouble Double.MAX_VALUE
        }

        expect:

        sb.with {
            assert readDouble() == Double.MAX_VALUE
            assert read() == 1 as byte
            assert read() == 1 as byte
            assert read() == 2 as byte
            assert readDouble() == Double.MAX_VALUE
        }
    }
}
