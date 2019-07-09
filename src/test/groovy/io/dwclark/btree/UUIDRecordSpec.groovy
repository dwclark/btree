package io.dwclark.btree

import spock.lang.*
import io.dwclark.btree.io.FixedBuffer

class UUIDRecordSpec extends Specification {

    def 'test place, extract, and compare'() {
        setup:
        def num = 10_000
        def record = UUIDRecord.instance();
        def fb = new FixedBuffer((int) (10_000 * record.size()), false);
        def bytes = fb.forWrite()
        def uuids = (0..<num).collect { UUID.randomUUID() }

        def pos = 0L;
        uuids.each { uuid -> record.place(bytes, pos, uuid); pos += record.size(); }

        expect:
        uuids.eachWithIndex { uuid, idx ->
            pos = idx * record.size()
            assert uuid == record.extract(bytes, pos)
        }
        
        (0..<(num-1)).each { idx ->
            def shouldBe = uuids[idx] <=> uuids[idx+1]
            assert shouldBe == record.compare(bytes, (idx+1) * record.size(), uuids[idx])
        }
    }

    def 'test basic compare'() {
        setup:
        def u1 = UUID.fromString('a08258d3-7a2d-49e8-a980-dd2f153a584a')
        def u2 = UUID.fromString('2b83bb69-cd09-4ed3-99f5-e1cda779f91a')
        def record = UUIDRecord.instance();
        def fb = new FixedBuffer(128, false);
        def bytes = fb.forWrite()
        record.place(bytes, 0L, u2)
        
        expect:
        (u1 <=> u2) ==  -1
        record.compare(bytes, 0L, u1) == -1
        (u2 <=> u1) == 1
    }
}
