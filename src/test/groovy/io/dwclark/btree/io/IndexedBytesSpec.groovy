package io.dwclark.btree.io

import spock.lang.*
import static java.nio.charset.StandardCharsets.UTF_8

class IndexedBytesSpec extends Specification {
    
    def "basic single buffer operations"() {
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

    def "channel buffers operations"() {
        setup:

        def id = "testing"
        def str = "to be or not to be...that is the question"
        def iVal = 400
        def lVal = 500L
        def file = File.createTempFile("test", ".bin");
        def cb = new ChannelBuffers(8, 1_024, ChannelBuffers.Locking.NONE)
        cb.createChannel(id, file)
        def sb = new SerialMutableBytes(cb.forWrite(id))

        when:
        sb.with {
            writeInt iVal
            writeLong lVal
            writeString "to be or not to be...that is the question"
        }

        int total = sb.writeAt

        then:
        sb.with {
            assert readInt() == iVal
            assert readLong() == lVal
            assert readString() == str
        }
        
        when:
        sb.stop()
        cb.shutdown()
        cb = new ChannelBuffers(8, 1_024, ChannelBuffers.Locking.NONE)
        cb.createChannel(id, file)
        sb = new SerialImmutableBytes(cb.forRead(id))

        then:

        sb.with {
            assert readInt() == iVal
            assert readLong() == lVal
            assert readString() == str
            assert readAt == total
        }
        
        cleanup:

        file.delete();
    }

    def 'test non-overlapping copy'() {
        setup:
        def fb = new FixedBuffer(128, false)
        def fw = fb.forWrite()
        for(int i = 0; i < 128; ++i) {
            fw.write((long) i, (byte) i)
        }

        fw.copy(64L, fw, 0, 64);

        expect:
        (0..63).every { i ->
            fw.read(i) == fw.read(i+64)
        }
    }

    def 'test overlapping copy right'() {
        setup:
        def size = 2_048
        def fb = new FixedBuffer(2048, false)
        def fw = fb.forWrite()
        for(int i = 0; i < 64; ++i) {
            fw.writeInt(i *32L, i)
        }

        fw.copy(256L, fw, 0L, 1536);

        (0..<8).each { i -> assert(fw.readInt(i*32L) == i) }
        (8..<56).each { i -> assert(fw.readInt(i*32L) == (i-8)) }
        (56..<64).each { i -> assert(fw.readInt(i*32L) == i) }
    }

    def 'test overlapping copy left'() {
        setup:
        def size = 2_048
        def fb = new FixedBuffer(2048, false)
        def fw = fb.forWrite()
        for(int i = 0; i < 64; ++i) {
            fw.writeInt(i *32L, i)
        }

        fw.copy(0L, fw, 256L, 1536);

        (0..<48).each { i -> assert(fw.readInt(i*32L) == (i + 8)) }
        (48..<64).each { i -> assert(fw.readInt(i*32L) == i) }
    }
}
