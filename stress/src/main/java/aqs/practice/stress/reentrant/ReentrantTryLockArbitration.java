package aqs.practice.stress.reentrant;

import aqs.practice.SimpleReentrantLock;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.ZZ_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/** Два потока одновременно делают tryLock и не отпускают: ровно один победитель. */
@JCStressTest
@Outcome(id = {"true, false", "false, true"}, expect = ACCEPTABLE, desc = "Ровно один победитель")
@Outcome(id = "true, true", expect = FORBIDDEN, desc = "Лок выдан двоим")
@Outcome(id = "false, false", expect = FORBIDDEN, desc = "Ложная неудача")
@State
public class ReentrantTryLockArbitration {

    private final SimpleReentrantLock lock = new SimpleReentrantLock();

    @Actor
    public void actor1(ZZ_Result r) {
        r.r1 = lock.tryLock();
    }

    @Actor
    public void actor2(ZZ_Result r) {
        r.r2 = lock.tryLock();
    }
}
