package io.dwclark.btree

import spock.lang.*
import io.dwclark.btree.io.GrowableBuffers
import io.dwclark.btree.io.FixedBuffer

class BTreeSpec extends Specification {

    static final lr = LongRecord.instance()
    
    def "test buffer size for min degree"() {
        setup:
        def for2 = NodeFactory.bufferSizeForMinDegree(2, lr, lr)
        def for3 = NodeFactory.bufferSizeForMinDegree(3, lr, lr)
        
        expect:
        for2 == 66
        for3 == 106
        new LongLongFactory(for2).minDegree == 2
        new LongLongFactory(for3).minDegree == 3
        new LongLongFactory(4096).minDegree == 102
    }
    
    def "test bad buffer size"() {
        when:
        new LongLongFactory(65)
        
        then:
        thrown IllegalArgumentException
    }

    def "test basic constructor"() {
        setup:
        def bufferSize = 4_096
        def factory = new LongLongFactory(4_096)
        def fb = new FixedBuffer(bufferSize, false)
        def btree = new BTree(fb, factory)
        
        expect:
        factory.minDegree == 102
        factory.minKeys == 101
        factory.minChildren == 102
        factory.maxKeys == 203
        factory.maxChildren == 204
        btree.root == 0
    }

    def "test leaf/count b-tree manipulation methods"() {
        setup:
        def bufferSize = 4_096
        def factory = new LongLongFactory(bufferSize)
        def fb = new FixedBuffer(bufferSize, false)
        def btree = new BTree(fb, factory);
        
        when:
        def node = btree.mutableRoot()
        
        then:
        node.leaf()
        node.count() == 0

        when:
        node.leaf(true)
        node.count(125)

        then:
        node.leaf()
        node.count() == 125
    }

    def "test place new b-tree manipulation methods"() {
        setup:
        def bufferSize = 4_096
        def fb = new FixedBuffer(bufferSize, false)
        def factory = new LongLongFactory(bufferSize)
        def btree = new BTree(fb, factory)
        def node = btree.mutableRoot()
        (0L..<13L).each { key ->
            node.key(key).value(key *2L).incrementCount().incrementIndex();
        }

        when:
        node.index(0)
        
        then:
        node.count() == 13
        (0L..<13L).every { index ->
            node.key() == (long) index && node.value() == (2L * index)
            node.incrementIndex()
        }
    }

    def "test child placement methods"() {
        setup:
        def bufferSize = 4_096
        def fb = new FixedBuffer(bufferSize, false)
        def factory = new LongLongFactory(bufferSize)
        def btree = new BTree(fb, factory)
        def node = btree.mutableRoot()
        
        when:
        (0..13).each { index -> node.child(index).incrementIndex() }
        node.index(0)

        then:
        (0..13).each { index ->
            assert node.child() == index
            node.incrementIndex()
        }
    }

    def "test shift"() {
        setup:
        def bufferSize = 4_096
        def fb = new FixedBuffer(bufferSize, false)
        def factory = new LongLongFactory(bufferSize);
        def btree = new BTree(fb, factory)
        def node = btree.mutableRoot()
        
        (0..<13).each { idx ->
            node.key((long) idx).value(idx * 2L).child(idx * 3).incrementCount().incrementIndex()
        }
        
        node.child(13 * 3)
        node.index(0)

        when:
        node.rightShift()
        node.key((long) Integer.MAX_VALUE).value((long) Integer.MAX_VALUE)
            .child(Integer.MAX_VALUE).incrementCount()
        node.index(1)

        then:
        node.count() == 14
        (1..13).each { idx ->
            assert(node.key() == idx - 1)
            assert(node.value() == (idx-1) * 2)
            assert (node.child() == (idx-1) * 3)
            node.incrementIndex()
        }
        
        
        node.child() == 39
        node.index(0).child() == Integer.MAX_VALUE
        node.key() == Integer.MAX_VALUE
        node.value() == Integer.MAX_VALUE
    }
    
    def 'test insert and search single'() {
        setup:
        def bufferSize = 1024;
        def fb = new FixedBuffer(bufferSize, false)
        def factory = new LongLongFactory(bufferSize)
        def btree = new BTree(fb, factory);

        when:
        btree.insert 1L, 2L

        then:
        btree.search(1L) == 2L;
    }

    def 'test 2-3-4 tree'() {
        setup:
        def bufferSize = NodeFactory.bufferSizeForMinDegree(2, lr, lr)
        def fb = new FixedBuffer(4_096, false);
        def factory = new LongLongFactory(bufferSize)
        def btree = new BTree(fb, factory);

        when:
        (1L..20L).each { btree.insert(it, it * 2L) }
        

        then:
        (1L..20L).each { btree.search(it) == it * 2L }
    }

    public List<String> toStrs(final List list) {
        list.collect { node ->
            node.keys().inject("") { str, lng -> str += (lng as char) as String } }
    }

    public List<String> valsToStrs(final List list) {
        list.collect { node ->
            node.values().inject("") { str, lng -> str += (lng as char) as String } }
    }

    def 'test book insert example'() {
        setup:
        def bufferSize = NodeFactory.bufferSizeForMinDegree(3, lr, lr)
        def fb = new FixedBuffer(4_096, false)
        def factory = new LongLongFactory(bufferSize)
        def btree = new BTree(fb, factory)
        def root = btree.mutableRoot().leaf(false)
        [ 'g', 'm', 'p', 'x' ].each { s ->
            root.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        def w = fb.forWrite()
        def node1 = btree.nextNode(w)
        [ 'a', 'c', 'd', 'e' ].each { s ->
            node1.with {
                key((s as char) as long)
                value((s as char) as long)
                incrementCount()
                incrementIndex()
            }
        }

        def node2 = btree.nextNode(w).leaf(true)
        [ 'j', 'k' ].each { s ->
            node2.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        def node3 = btree.nextNode(w).leaf(true)
        [ 'n', 'o' ].each { s ->
            node3.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        def node4 = btree.nextNode(w).leaf(true)
        [ 'r', 's', 't', 'u', 'v' ].each { s ->
            node4.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        def node5 = btree.nextNode(w).leaf(true)
        [ 'y', 'z' ].each { s ->
            node5.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        when:
        root.with {
            index(0)
            leftChild(node1).incrementIndex()
            leftChild(node2).incrementIndex()
            leftChild(node3).incrementIndex()
            leftChild(node4).incrementIndex()
            leftChild(node5)
        }

        then:
        toStrs(btree.breadthFirst()) == [ 'gmpx', 'acde', 'jk', 'no', 'rstuv', 'yz' ]

        when:
        btree.insert(('b' as char) as long, ('b' as char) as long)

        then:
        toStrs(btree.breadthFirst()) == [ 'gmpx', 'abcde', 'jk', 'no', 'rstuv', 'yz' ]

        when:
        btree.insert(('q' as char) as long, ('q' as char) as long)

        then:
        toStrs(btree.breadthFirst()) == [ 'gmptx', 'abcde', 'jk', 'no', 'qrs', 'uv', 'yz' ]

        when:
        btree.insert(('l' as char) as long, ('l' as char) as long)

        then:
        toStrs(btree.breadthFirst()) == [ 'p', 'gm', 'tx', 'abcde', 'jkl', 'no', 'qrs', 'uv', 'yz' ]

        when:
        btree.insert(('f' as char) as long, ('f' as char) as long)

        then:
        valsToStrs(btree.breadthFirst()) == [ 'p', 'cgm', 'tx', 'ab', 'def', 'jkl', 'no', 'qrs', 'uv', 'yz' ]
    }

    def 'test leaf based removal'() {
        setup:
        def bufferSize = 4_096
        def fb = new FixedBuffer(bufferSize, false)
        def factory = new LongLongFactory(bufferSize);
        def btree = new BTree(fb, factory)

        (1L..10L).each { btree.insert it, it }
        def toRemove = (1L..10L).toList()
        Collections.shuffle(toRemove)

        expect:
        toRemove.every { lng ->
            def found = btree.search(lng) != null;
            btree.remove(lng);
            found && btree.search(lng) == null
        }
    }
    
    def 'test book remove example'() {
        setup:
        def bufferSize = NodeFactory.bufferSizeForMinDegree(3, lr, lr)
        def fb = new FixedBuffer(4_096, false)
        def factory = new LongLongFactory(bufferSize)
        def btree = new BTree(fb, factory)
        def root = btree.mutableRoot().leaf(false)
        def w = fb.forWrite()
       
        [ 'p' ].each { s ->
            root.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        def level2_1 = btree.nextNode(w).leaf(false)
        [ 'c', 'g', 'm' ].each { s ->
            level2_1.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        def level2_2 = btree.nextNode(w).leaf(false)
        [ 't', 'x' ].each { s ->
            level2_2.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }
       
        def level3_1 = btree.nextNode(w).leaf(true)
        [ 'a', 'b' ].each { s ->
            level3_1.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        def level3_2 = btree.nextNode(w).leaf(true)
        [ 'd', 'e', 'f' ].each { s ->
            level3_2.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }
       
        def level3_3 = btree.nextNode(w).leaf(true)
        [ 'j', 'k', 'l' ].each { s ->
            level3_3.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        def level3_4 = btree.nextNode(w).leaf(true)
        [ 'n', 'o' ].each { s ->
            level3_4.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        def level3_5 = btree.nextNode(w).leaf(true)
        [ 'q', 'r', 's' ].each { s ->
            level3_5.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        def level3_6 = btree.nextNode(w).leaf(true)
        [ 'u', 'v' ].each { s ->
            level3_6.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        def level3_7 = btree.nextNode(w).leaf(true)
        [ 'y', 'z' ].each { s ->
            level3_7.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        when:
        root.index(0).leftChild(level2_1).rightChild(level2_2)
        level2_1.index(0).leftChild(level3_1).index(1).leftChild(level3_2)
        level2_1.index(2).leftChild(level3_3).index(3).leftChild(level3_4)
        level2_2.index(0).leftChild(level3_5).index(1).leftChild(level3_6)
        level2_2.index(2).leftChild(level3_7);

        then:
        toStrs(btree.breadthFirst()) == ['p', 'cgm', 'tx', 'ab', 'def', 'jkl', 'no', 'qrs', 'uv', 'yz']

        when:
        def removed = btree.remove(('f' as char) as long)
       
        then:
        removed
        toStrs(btree.breadthFirst()) == ['p', 'cgm', 'tx', 'ab', 'de', 'jkl', 'no', 'qrs', 'uv', 'yz']

        when:
        removed = btree.remove(('m' as char) as long)

        then:
        removed
        toStrs(btree.breadthFirst()) == ['p', 'cgl', 'tx', 'ab', 'de', 'jk', 'no', 'qrs', 'uv', 'yz']

        when:
        removed = btree.remove(('g' as char) as long)

        then:
        removed
        toStrs(btree.breadthFirst()) == ['p', 'cl', 'tx', 'ab', 'dejk', 'no', 'qrs', 'uv', 'yz']

        when:
        removed = btree.remove(('d' as char) as long)
        
        then:
        removed
        toStrs(btree.breadthFirst()) == ['clptx', 'ab', 'ejk', 'no', 'qrs', 'uv', 'yz']

        when:
        removed = btree.remove(('b' as char) as long)

        then:
        removed
        toStrs(btree.breadthFirst()) == ['elptx', 'ac', 'jk', 'no', 'qrs', 'uv', 'yz']
    }

    def 'test basic to string'() {
        setup:
        def bufferSize = NodeFactory.bufferSizeForMinDegree(2, lr, lr)
        def fb = new FixedBuffer(4_096, false)
        def factory = new LongLongFactory(bufferSize)
        def btree = new BTree(fb, factory)
        btree.insert(1L, 2L)
        btree.insert(3L, 4L)
        btree.insert(5L, 6L)
        btree.insert(7L, 8L)
        def str = btree.toString();

        expect:
        str
        (1..8).every { str.contains(it as String) }
    }

    def 'test remove missing'() {
        setup:
        def bufferSize = NodeFactory.bufferSizeForMinDegree(3, lr, lr)
        def fb = new FixedBuffer(4_096, false)
        def factory = new LongLongFactory(bufferSize)
        def btree = new BTree(fb, factory)
        btree.insert(1L, 2L)
        btree.insert(3L, 4L)
        btree.insert(5L, 6L)
        btree.insert(7L, 8L)

        expect:
        !btree.remove(10L)
        !btree.remove(100L);
    }

    def 'test update'() {
        setup:
        def bufferSize = NodeFactory.bufferSizeForMinDegree(3, lr, lr)
        def fb = new FixedBuffer(4_096, false)
        def factory = new LongLongFactory(bufferSize)
        def btree = new BTree(fb, factory)
        btree.insert(1L, 2L)
        btree.insert(3L, 4L)
        btree.insert(5L, 6L)
        btree.insert(7L, 8L)

        when:
        btree.insert 7L, 17L

        then:
        btree.search(7L) == 17L;
    }

    def 'misc inner node remove (missing from book example)'() {
        setup:
        def bufferSize = NodeFactory.bufferSizeForMinDegree(3, lr, lr)
        def fb = new FixedBuffer(4_096, false)
        def btree = new BTree(fb, new LongLongFactory(bufferSize))
        def root = btree.mutableRoot().leaf(false)
        def w = fb.forWrite()
       
        [ 'g', 'k', 'n' ].each { s ->
            root.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        def node1 = btree.nextNode(w).leaf(true)
        [ 'a', 'b' ].each { s ->
            node1.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        def node2 = btree.nextNode(w).leaf(true)
        [ 'h', 'i' ].each { s ->
            node2.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        def node3 = btree.nextNode(w).leaf(true)
        [ 'l', 'm' ].each { s ->
            node3.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        def node4 = btree.nextNode(w).leaf(true)
        [ 'o', 'p', 'q' ].each { s ->
            node4.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        when:
        root.with {
            index(0)
            leftChild(node1).incrementIndex().leftChild(node2).incrementIndex()
            leftChild(node3).incrementIndex().leftChild(node4)
        }

        then:
        toStrs(btree.breadthFirst()) == ['gkn', 'ab', 'hi', 'lm', 'opq' ]
        btree.toString() != null;

        when:
        def removed = btree.remove(('n' as char) as long)

        then:
        removed
        toStrs(btree.breadthFirst()) == ['gko', 'ab', 'hi', 'lm', 'pq' ]
        
    }

    def 'misc inner node remove 2 (missing from book example)'() {
        setup:
        def bufferSize = NodeFactory.bufferSizeForMinDegree(3, lr, lr)
        def fb = new FixedBuffer(4_096, false)
        def btree = new BTree(fb, new LongLongFactory(bufferSize))
        def root = btree.mutableRoot().leaf(false)
        def w = fb.forWrite()
       
        [ 'g', 'k' ].each { s ->
            root.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        def node1 = btree.nextNode(w).leaf(true)
        [ 'a', 'b', 'c' ].each { s ->
            node1.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        def node2 = btree.nextNode(w).leaf(true)
        [ 'h', 'i' ].each { s ->
            node2.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        def node3 = btree.nextNode(w).leaf(true)
        [ 'l', 'm' ].each { s ->
            node3.key((s as char) as long).value((s as char) as long).incrementCount().incrementIndex()
        }

        when:
        root.with {
            index(0)
            leftChild(node1).incrementIndex()
            leftChild(node2).incrementIndex()
            leftChild(node3)
        }

        then:
        toStrs(btree.breadthFirst()) == [ 'gk', 'abc', 'hi', 'lm' ]

        when:
        def removed = btree.remove(('i' as char) as long)

        then:
        toStrs(btree.breadthFirst()) == [ 'ck', 'ab', 'gh', 'lm' ]

        when:
        removed = btree.remove(('g' as char) as long)

        then:
        toStrs(btree.breadthFirst()) == [ 'k', 'abch', 'lm' ]
    }
}
