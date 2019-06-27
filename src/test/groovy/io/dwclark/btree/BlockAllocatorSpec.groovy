package io.dwclark.btree

import spock.lang.*

class BlockAllocatorSpec extends Specification {

    def 'test basic ops'() {
        setup:
        def alloc = new BlockAllocator(0xFFFF_FFFFL, false)

        when:
        true

        then:
        alloc.next() == 0L
        alloc.next() == 1L
        alloc.next() == 2L
        alloc.next() == 3L

        when:
        alloc.unused(0L)
        alloc.unused(2L)

        then:
        alloc.next() == 0L
        alloc.next() == 2L
        alloc.next() == 4L
    }

    def 'test exhaustion'() {
        setup:
        def alloc = new BlockAllocator(4L, false);

        when:
        true

        then:
        alloc.next() == 0L
        alloc.next() == 1L
        alloc.next() == 2L
        alloc.next() == 3L

        when:
        alloc.next()

        then:
        thrown IllegalStateException
    }

    def 'test negative block'() {
        setup:
        def alloc = new BlockAllocator(4L, false);

        when:
        alloc.unused(-1L)

        then:
        thrown IllegalArgumentException

        when:
        alloc.unused(0L)

        then:
        thrown IllegalArgumentException
        
    }
}
