package aqs.practice.stress.semaphore;

import aqs.practice.SimpleSemaphore;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.ZZZ_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/** Same as {@link SemaphoreTryAcquireArbitration}, but for fair mode. */
@JCStressTest
@Outcome(id = {"true, true, false", "true, false, true", "false, true, true"},
        expect = ACCEPTABLE, desc = "Exactly two got a permit")
@Outcome(id = "true, true, true", expect = FORBIDDEN, desc = "More permits were handed out than existed")
@Outcome(expect = FORBIDDEN, desc = "Spurious failure: there were permits and no queue, but tryAcquire returned false")
@State
public class SemaphoreFairTryAcquireArbitration {

    private final SimpleSemaphore semaphore = new SimpleSemaphore(2, true);

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
