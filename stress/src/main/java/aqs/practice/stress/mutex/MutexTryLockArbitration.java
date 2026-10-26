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
 * Два потока одновременно делают tryLock на свободном локе и не отпускают его.
 * Ровно один должен выиграть.
 */
@JCStressTest
@Outcome(id = {"true, false", "false, true"}, expect = ACCEPTABLE, desc = "Ровно один победитель")
@Outcome(id = "true, true", expect = FORBIDDEN, desc = "Лок выдан двоим")
@Outcome(id = "false, false", expect = FORBIDDEN, desc = "Ложная неудача: лок был свободен, но не достался никому")
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
