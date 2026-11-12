package aqs.practice.stress.semaphore;

import aqs.practice.SimpleSemaphore;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.ZZZ_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/**
 * Two permits, three threads call tryAcquire() at the same time.
 * Exactly two must win: no more (too many handed out), no fewer (spurious failure on CAS).
 */
@JCStressTest
@Outcome(id = {"true, true, false", "true, false, true", "false, true, true"},
        expect = ACCEPTABLE, desc = "Exactly two got a permit")
@Outcome(id = "true, true, true", expect = FORBIDDEN, desc = "More permits were handed out than existed")
@Outcome(expect = FORBIDDEN, desc = "Spurious failure: there were permits, but tryAcquire returned false")
@State
public class SemaphoreTryAcquireArbitration {

    private final SimpleSemaphore semaphore = new SimpleSemaphore(2);

    @Actor
    public void actor1(ZZZ_Result r) {
        r.r1 = semaphore.tryAcquire();
    }

    @Actor
    public void actor2(ZZZ_Result r) {
        r.r2 = semaphore.tryAcquire();
    }

    @Actor
    public void actor3(ZZZ_Result r) {
        r.r3 = semaphore.tryAcquire();
    }
}
