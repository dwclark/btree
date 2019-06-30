package io.dwclark.btree

import spock.lang.*
import static StandardBTree.*
import io.dwclark.btree.io.GrowableBuffers
import io.dwclark.btree.io.FixedBuffer

class StandardBTreeSpec extends Specification {

    def "test min degree computation"() {
        expect:
        findMinDegree(4096) == 102
        findMinDegree(86) == 2
    }

    def "test bad buffer size"() {
        when:
        findMinDegree(85)

        then:
        thrown IllegalArgumentException
    }

    def "test basic constructor"() {
        setup:
        def bufferSize = 4_096;
        def view = new GrowableBuffers(bufferSize, false);
        def btree = new StandardBTree(bufferSize, view);

        expect:
        btree.minDegree == 102
        btree.minKeys == 101
        btree.minChildren == 102
        btree.maxKeys == 203
        btree.maxChildren == 204
        btree.root == 0
        btree.rootBase == 0L
    }

    def "test leaf/count b-tree manipulation methods"() {
        setup:
        def bufferSize = 4_096
        def fb = new FixedBuffer(bufferSize, false)

        when:
        def bytes = fb.forWrite()
        
        then:
        !leaf(bytes, 0L)
        count(bytes, 0L) == 0

        when:
        leaf(bytes, 0L, true)
        count(bytes, 0L, 125)

        then:
        leaf(bytes, 0)
        count(bytes, 0L) == 125
        bytes.readShort(0L) < 0
    }

    def "test place new b-tree manipulation methods"() {
        setup:
        def bufferSize = 4_096
        def fb = new FixedBuffer(bufferSize, false)
        def bytes = fb.forWrite()
        (0..<13).each { key ->
            placeNew(bytes, 0L, key, (long) key, key * 2L)
        }

        expect:
        (0..<13).every { index -> key(bytes, 0L, index) == (long) index }
        (0..<13).every { index -> value(bytes, 0L, index) == (2L * index) }
        count(bytes, 0L) == 13
    }

    def "test key/value b-tree manipulation methods"() {
        setup:
        def bufferSize = 4_096
        def fb = new FixedBuffer(bufferSize, false)
        def bytes = fb.forWrite()
        (0..<10).each { k ->
            key(bytes, 0L, k, (long) k)
            value(bytes, 0L, k, k * 2L)
        }
        
        expect:
        (0..<10).every { index -> key(bytes, 0L, index) == (long) index }
        (0..<10).every { index -> value(bytes, 0L, index) == (2L * index) }
    }

    def "test child placement methods"() {
        setup:
        def bufferSize = 4_096
        def fb = new FixedBuffer(bufferSize, false)
        def bytes = fb.forWrite()
        (0..<13).each { key ->
            placeNew(bytes, 0L, key, (long) key, key * 2L)
        }

        when:
        (0..13).each { index -> child(bytes, 0L, index, index * 3) }

        then:
        (0..13).every { index -> child(bytes, 0L, index) == index * 3 }   
    }

    def "test shift"() {
        setup:
        def bufferSize = 4_096
        def fb = new FixedBuffer(bufferSize, false)
        def bytes = fb.forWrite()
        (0..<13).each { idx ->
            placeNew(bytes, 0L, idx, (long) idx, idx * 2L)
            child(bytes, 0L, idx, idx * 3)
        }

        child(bytes, 0L, 13, 13 * 3)

        when:
        shiftRecord(bytes, 0L, 0)
        placeNew(bytes, 0L, 0, (long) Integer.MAX_VALUE, (long) Integer.MAX_VALUE)
        child(bytes, 0L, 0, Integer.MAX_VALUE)

        then:
        count(bytes, 0L) == 14
        (1..13).every { idx -> key(bytes, 0L, idx) == idx - 1 }
        (1..13).every { idx -> value(bytes, 0L, idx) == (idx-1) * 2 }
        (1..13).every { idx -> child(bytes, 0L, idx) == (idx-1) * 3 }
        child(bytes, 0L, 14) == 39
        child(bytes, 0L, 0) == Integer.MAX_VALUE
        key(bytes, 0L, 0) == Integer.MAX_VALUE
        value(bytes, 0L, 0) == Integer.MAX_VALUE
    }
}
