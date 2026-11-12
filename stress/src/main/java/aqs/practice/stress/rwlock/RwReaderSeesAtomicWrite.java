package aqs.practice.stress.rwlock;

import aqs.practice.SimpleReadWriteLock;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/** The writer writes x, y under the write lock; the reader under the read lock sees either both old or both new values. */
@JCStressTest
@Outcome(id = "0, 0", expect = ACCEPTABLE, desc = "The reader got in before the writer")
@Outcome(id = "1, 1", expect = ACCEPTABLE, desc = "The reader got in after the writer")
@Outcome(expect = FORBIDDEN, desc = "The reader saw a half-written state")
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
