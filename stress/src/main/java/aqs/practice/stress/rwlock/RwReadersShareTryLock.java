package aqs.practice.stress.rwlock;

import aqs.practice.SimpleReadWriteLock;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.ZZ_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/**
 * Two readers call tryLock on a free lock at the same time.
 * Both must get in: one of their CASes may lose, and that is no reason to refuse.
 */
@JCStressTest
@Outcome(id = "true, true", expect = ACCEPTABLE, desc = "Both readers are inside")
@Outcome(expect = FORBIDDEN, desc = "A reader was refused with no writer around: spurious failure on CAS")
@State
public class RwReadersShareTryLock {

    private final SimpleReadWriteLock rw = new SimpleReadWriteLock();

    @Actor
    public void reader1(ZZ_Result r) {
        r.r1 = rw.readLock().tryLock();
    }

    @Actor
    public void reader2(ZZ_Result r) {
        r.r2 = rw.readLock().tryLock();
    }
}
