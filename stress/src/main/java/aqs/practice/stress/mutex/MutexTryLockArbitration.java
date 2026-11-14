package aqs.practice.stress.mutex;

import aqs.practice.Mutex;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.ZZ_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/**
 * Two threads call tryLock on a free lock at the same time and do not release it.
 * Exactly one must win.
 */
@JCStressTest
@Outcome(id = {"true, false", "false, true"}, expect = ACCEPTABLE, desc = "Exactly one winner")
@Outcome(id = "true, true", expect = FORBIDDEN, desc = "The lock was given to two threads")
@Outcome(id = "false, false", expect = FORBIDDEN, desc = "Spurious failure: the lock was free but nobody got it")
@State
public class MutexTryLockArbitration {

    private final Mutex lock = new Mutex();

    @Actor
    public void actor1(ZZ_Result r) {
        r.r1 = lock.tryLock();
    }

    @Actor
    public void actor2(ZZ_Result r) {
        r.r2 = lock.tryLock();
    }
}
