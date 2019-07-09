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
        toStrs(btree.breadthFirstNodes()) == [ 'gmpx', 'acde', 'jk', 'no', 'rstuv', 'yz' ]

        when:
        btree.insert(('b' as char) as long, ('b' as char) as long)

        then:
        toStrs(btree.breadthFirstNodes()) == [ 'gmpx', 'abcde', 'jk', 'no', 'rstuv', 'yz' ]

        when:
        btree.insert(('q' as char) as long, ('q' as char) as long)

        then:
        toStrs(btree.breadthFirstNodes()) == [ 'gmptx', 'abcde', 'jk', 'no', 'qrs', 'uv', 'yz' ]

        when:
        btree.insert(('l' as char) as long, ('l' as char) as long)

        then:
        toStrs(btree.breadthFirstNodes()) == [ 'p', 'gm', 'tx', 'abcde', 'jkl', 'no', 'qrs', 'uv', 'yz' ]

        when:
        btree.insert(('f' as char) as long, ('f' as char) as long)

        then:
        valsToStrs(btree.breadthFirstNodes()) == [ 'p', 'cgm', 'tx', 'ab', 'def', 'jkl', 'no', 'qrs', 'uv', 'yz' ]
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
        toStrs(btree.breadthFirstNodes()) == ['p', 'cgm', 'tx', 'ab', 'def', 'jkl', 'no', 'qrs', 'uv', 'yz']

        when:
        def removed = btree.remove(('f' as char) as long)
       
        then:
        removed
        toStrs(btree.breadthFirstNodes()) == ['p', 'cgm', 'tx', 'ab', 'de', 'jkl', 'no', 'qrs', 'uv', 'yz']

        when:
        removed = btree.remove(('m' as char) as long)

        then:
        removed
        toStrs(btree.breadthFirstNodes()) == ['p', 'cgl', 'tx', 'ab', 'de', 'jk', 'no', 'qrs', 'uv', 'yz']

        when:
        removed = btree.remove(('g' as char) as long)

        then:
        removed
        toStrs(btree.breadthFirstNodes()) == ['p', 'cl', 'tx', 'ab', 'dejk', 'no', 'qrs', 'uv', 'yz']

        when:
        removed = btree.remove(('d' as char) as long)
        
        then:
        removed
        toStrs(btree.breadthFirstNodes()) == ['clptx', 'ab', 'ejk', 'no', 'qrs', 'uv', 'yz']

        when:
        removed = btree.remove(('b' as char) as long)

        then:
        removed
        toStrs(btree.breadthFirstNodes()) == ['elptx', 'ac', 'jk', 'no', 'qrs', 'uv', 'yz']
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
        toStrs(btree.breadthFirstNodes()) == ['gkn', 'ab', 'hi', 'lm', 'opq' ]
        btree.toString() != null;

        when:
        def removed = btree.remove(('n' as char) as long)

        then:
        removed
        toStrs(btree.breadthFirstNodes()) == ['gko', 'ab', 'hi', 'lm', 'pq' ]
        
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
        btree.valid
        toStrs(btree.breadthFirstNodes()) == [ 'gk', 'abc', 'hi', 'lm' ]

        when:
        def removed = btree.remove(('i' as char) as long)

        then:
        btree.valid
        toStrs(btree.breadthFirstNodes()) == [ 'ck', 'ab', 'gh', 'lm' ]

        when:
        removed = btree.remove(('g' as char) as long)

        then:
        btree.valid
        toStrs(btree.breadthFirstNodes()) == [ 'k', 'abch', 'lm' ]
    }

    def 'test uuid/int btree'() {
        setup:
        def bufferSize = 4_096
        def factory = new StandardFactory(UUIDRecord.instance(), IntegerRecord.instance(), bufferSize);
        def fb = new FixedBuffer(bufferSize, false)
        def btree = new BTree(fb, factory)

        def uuids = [ '33f7ab03-bd5a-4589-a8cc-2e0a62cdeb6c', '0678c984-2121-485c-b7ce-fecd6f90500f', 'fa5db297-007d-4d8a-971e-d4f01a1ca95c', 'b75a1c48-6b35-43b4-9d2c-2423db3fe0be', 'e33be3f8-3bb1-49d1-a6b9-76647d53b269', 'cc36fc06-140c-4ac5-a9e1-e141358c88dd', '399b86a8-3f7a-4922-8c13-5d5cb290c649', '10fa535e-289c-4ba8-86e8-35c5db888fda', '9ff75873-7e08-4ee3-9dd0-ccae0b6a3d03', '4fd664b8-3fce-4e34-a858-74d11c79287e', 'd7c556d1-b42d-403e-87bd-1a535d059c7e', '0b73bab6-6e8f-4979-913b-9f2aad4fbb7a', '1d4684a0-6030-40fe-8079-0b1eec658dc7', '4abe331b-f2e1-4847-8113-83bc0879bf8d', '4fb9f81f-0625-445f-af60-769436dc111b', '34b1ce35-8c81-47c5-b82c-7d4fc2b7035e', '43f81cc5-51ac-4c2e-86cf-c8b571060a3d', 'ac5b8a2f-f12b-461a-bf9a-a0f55504444e', '1bd59d7f-2096-40a3-8a89-800883cf79bb', '75bdec88-223c-41c9-a77d-13c5a16060f5', 'fd70a753-6032-422a-b59f-3895b19c67bc', 'd4d66d83-3242-4c45-8514-37a5cae579c7', '40e3cae6-f48b-4048-8604-37e9aea9b1aa', '86dacb80-5ed0-4bcc-8cc9-1e4466cee623', 'f5cb1475-bb49-42c6-965f-b88b9b5fdfd6', 'baebdc3a-73b6-45a0-af39-23239d5ef303', '1bc743bd-31b9-4324-b8ec-e2611ccc6da5', '05f77598-cd82-40a4-8369-0e7681e58fb8', '259d97c1-d6b2-461a-a0af-103bf9da507c', 'f3b4c720-6d07-432a-921f-ddf9f396da17', '32be1672-0542-4241-9906-a532ecc5b77f', '07db3764-d215-464f-8591-b04987ec78c4', 'f3ca18ba-840e-4d9a-8b03-7d5aa65337ca', '29583a38-ba56-4096-bc7d-c4e61ab45f2e', '107673b0-d0f2-4889-87de-f9d1793ae9bc', 'e48c45bf-880d-4d1a-be91-1bedad9761d2', 'dd96656c-7b78-4cbd-bb41-356e54147500', 'e52859e6-9857-4c8d-b894-65f03b2bec35', '2907acec-1df0-4561-966f-c6f86fa9be28', '318880a0-9c85-4c94-b95a-1c3bb4505fe8', '452d55b6-734a-4ad4-8771-fa2e0c91c1c8', '8359d13b-c868-4150-9b92-727bafda63ba', 'e881155a-c4d4-4857-802f-e530aa55fd16', 'e46bc6ad-cf36-4f28-8c20-b18b9331965a', '4423e0a3-5480-4e12-92b1-3446f1e3482e', '83dd9dc0-17d0-4174-9eab-9cd98650e456', 'd820dae1-8694-497a-a098-d9cd0a4c3e65', 'e860863c-5ba0-4213-8ed3-0c229ee2e747', 'c6476459-58cb-41fe-ab80-316cfd9d5788', 'bbfb5f86-9571-4408-b813-d028ecfa4738', '3f2f4b95-4eab-410e-b338-51afc8fa535b', 'c1285b98-a5a6-4595-8c32-f2b821fecd9c', '322c78ec-c5a2-4e6b-8637-edd29690e66d', '5f8e99fc-1170-42e0-aa90-8cda22bf74b4', '6fa5eea4-447d-425a-a6cc-6183b9f4a2d9', '29f8ff50-0998-450c-b764-094326dce6d2', '8c56b293-1b99-4f6e-8a82-a19f11285942', '6e921d2f-4d89-4303-adae-a05ac3a5cb70', '0789a463-3b71-4682-a0f7-6a24d0ad62ec', '9d49580f-ee4f-48d1-8de7-ffa80442cd00', 'cd3dd62a-2e50-4d93-9bb3-3130cc59ce6f', '8ea00ff5-50d1-453f-b4fb-c3beee63bb1c', 'dbd91a33-3680-4201-a206-bbcfb75eac5a', '280076ab-fbf1-4402-b990-6a91adcb2895', '0fe1e1cd-ca82-488d-b003-439b040f417d', '3ab404a0-80cb-476b-b821-b573f24778bf', '78fc6bd9-1fd0-4863-ad83-92d42c1e7e00', 'd8c7c0af-7103-43a6-9f29-60abddcefc6f', '4205ddfd-6e29-4607-b2d3-1a355c9567ec', '914a2135-37bb-4639-9054-37452cd1c0bb', '425bbcbd-557d-428d-99c7-a40615bc40a2', 'ce8e3eca-78fe-456e-8c6c-d93dc127e44d', '3c65101f-56a2-489f-ad7f-611dbbed35e0', '8196188f-ee84-4bca-8e6b-1fe601ddc307', '7dbd9db0-dded-47c8-a2aa-41c1faf2d297'].collect { UUID.fromString(it) };
        
        uuids.eachWithIndex { uuid, idx ->
            btree.insert(uuid, (idx + 1))
            assert btree.valid
        }

        def counter = 75
        def problem = UUID.fromString('d4d66d83-3242-4c45-8514-37a5cae579c7')
        def uuidsSet = uuids as Set
        
        expect:
        uuids.every { uuid -> btree.search(uuid) }
        uuids.every { uuid -> btree.remove(uuid) }
    }

    def 'test random int/int btree'() {
        def ir = IntegerRecord.instance()
        def total = 1000
        def list = (0..<total).toList()
        Collections.shuffle(list);
        def bufferSize = NodeFactory.bufferSizeForMinDegree(3, ir, ir)
        def factory = new StandardFactory(ir, ir, bufferSize);
        def fb = new GrowableBuffers(128, false)
        def btree = new BTree(fb, factory)
        list.each { i ->
            btree.insert(i, i)
        }
        
        expect:
        list.size() == total
        list.every { i -> btree.search(i) == i }
        btree.search(-10) == null
        list.every { i -> btree.remove(i) }

    }

    def 'test random long/long btree'() {
        def total = 1000
        def list = (0..<total).toList()
        Collections.shuffle(list);
        def bufferSize = NodeFactory.bufferSizeForMinDegree(3, lr, lr)
        def factory = new LongLongFactory(bufferSize);
        def fb = new GrowableBuffers(128, false)
        def btree = new BTree(fb, factory)
        list.each { i ->
            btree.insert((long) i, (long) i)
        }
        
        expect:
        list.size() == total
        list.every { i -> btree.search((long) i) == i }
        btree.search(-10L) == null
        list.every { i -> btree.remove((long) i) }
    }

    def 'test bad btree successor bug'() {
        def list = [16, 34, 22, 4, 1, 8, 23, 11, 6, 18, 21, 31, 19, 9, 15, 17, 3, 28, 24,
                    27, 5, 26, 12, 20, 25, 32, 29, 7, 14, 10, 33, 30, 13, 0, 2]
        def bufferSize = NodeFactory.bufferSizeForMinDegree(3, lr, lr)
        def factory = new LongLongFactory(bufferSize);
        def fb = new GrowableBuffers(128, false)
        def btree = new BTree(fb, factory)
        list.each { i ->
            btree.insert((long) i, (long) i)
            assert btree.valid
        }
        
        expect:
        list.every { i -> btree.search((long) i) == i }
        btree.search(-10L) == null
        list.every { i ->
            btree.remove((long) i) && btree.valid
        }
    }
}
