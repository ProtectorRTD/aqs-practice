package aqs.practice.stress.rwlock;

import aqs.practice.SimpleReadWriteLock;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/** Писатель пишет x, y под write-локом; читатель под read-локом видит либо оба старых, либо оба новых. */
@JCStressTest
@Outcome(id = "0, 0", expect = ACCEPTABLE, desc = "Читатель зашёл раньше писателя")
@Outcome(id = "1, 1", expect = ACCEPTABLE, desc = "Читатель зашёл после писателя")
@Outcome(expect = FORBIDDEN, desc = "Читатель увидел полузаписанное состояние")
@State
public class RwReaderSeesAtomicWrite {

    private final SimpleReadWriteLock rw = new SimpleReadWriteLock();
    private int x;
    private int y;

    @Actor
    public void writer() {
        rw.writeLock().lock();
        try {
            x = 1;
            y = 1;
        } finally {
            rw.writeLock().unlock();
        }
    }

    @Actor
    public void reader(II_Result r) {
        rw.readLock().lock();
        try {
            r.r1 = y;
            r.r2 = x;
        } finally {
            rw.readLock().unlock();
        }
    }
}
