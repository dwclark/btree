package io.dwclark.btree.io;

import java.io.*
import spock.lang.*
import io.dwclark.btree.*

class ChannelBuffersSpec extends Specification {

    def 'basic btree on channel buffers'() {
        setup:
        def total = 10_000L
        def bufferSize = 4_096
        def name = 'mybtree'
        def file = File.createTempFile('tst', '.idx')
        file.deleteOnExit()
        def cb = new ChannelBuffers(bufferSize, 1_024, ChannelBuffers.Locking.NONE)
        cb.createChannel(name, file)
        def factory = new LongLongFactory(bufferSize);
        def btree = new BTree(cb.viewBytes(name), factory)

        when:
        (0L..<total).each { btree.insert it, it }

        then:
        (0L..<total).each { assert(btree.search(it) == it) }

        when:
        cb.flush()
        cb.shutdown()
        cb = new ChannelBuffers(bufferSize, 1_024, ChannelBuffers.Locking.NONE)
        cb.createChannel(name, file);
        def root = btree.root;
        def current = btree.allocator.current;
        def unused = btree.allocator.unused;
        def allocator = new BlockAllocator(0xFFFF_FFFFL, current, unused, false)
        btree = new BTree(cb.viewBytes(name), allocator, factory, root);
        
        then:
        (0L..<total).each { assert(btree.search(it) == it) }

        cleanup:
        file.delete()        
    }
}
