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

/** Два писателя делают x++ под write-локом. */
@JCStressTest
@Outcome(id = "2", expect = ACCEPTABLE, desc = "Оба инкремента на месте")
@Outcome(expect = FORBIDDEN, desc = "Два писателя были внутри одновременно")
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
