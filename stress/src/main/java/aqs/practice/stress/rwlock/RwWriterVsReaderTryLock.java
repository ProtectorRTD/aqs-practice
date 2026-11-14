package aqs.practice.stress.rwlock;

import aqs.practice.SimpleReadWriteLock;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.ZZ_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/** A writer and a reader call tryLock on a free lock at the same time: exactly one gets in. */
@JCStressTest
@Outcome(id = {"true, false", "false, true"}, expect = ACCEPTABLE, desc = "Exactly one got in")
@Outcome(id = "true, true", expect = FORBIDDEN, desc = "A writer and a reader are inside at the same time")
@Outcome(id = "false, false", expect = FORBIDDEN, desc = "Spurious failure: the lock was free")
@State
public class RwWriterVsReaderTryLock {

    private final SimpleReadWriteLock rw = new SimpleReadWriteLock();

    @Actor
    public void writer(ZZ_Result r) {
        r.r1 = rw.writeLock().tryLock();
    }

    @Actor
    public void reader(ZZ_Result r) {
        r.r2 = rw.readLock().tryLock();
    }
}
