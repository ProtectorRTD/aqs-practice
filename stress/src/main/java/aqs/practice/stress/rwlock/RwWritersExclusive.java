package aqs.practice.stress.rwlock;

import aqs.practice.SimpleReadWriteLock;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/** Two writers do x++ under the write lock. */
@JCStressTest
@Outcome(id = "2", expect = ACCEPTABLE, desc = "Both increments are there")
@Outcome(expect = FORBIDDEN, desc = "Two writers were inside at the same time")
@State
public class RwWritersExclusive {

    private final SimpleReadWriteLock rw = new SimpleReadWriteLock();
    private int x;

    @Actor
    public void writer1() {
        rw.writeLock().lock();
        try {
            x++;
        } finally {
            rw.writeLock().unlock();
        }
    }

    @Actor
    public void writer2() {
        rw.writeLock().lock();
        try {
            x++;
        } finally {
            rw.writeLock().unlock();
        }
    }

    @Arbiter
    public void arbiter(I_Result r) {
        r.r1 = x;
    }
}
