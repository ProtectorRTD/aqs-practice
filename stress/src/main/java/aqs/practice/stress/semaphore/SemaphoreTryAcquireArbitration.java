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
 * Два разрешения, три потока одновременно делают tryAcquire().
 * Должно выиграть ровно двое: ни больше (выдали лишнее), ни меньше (ложная неудача на CAS).
 */
@JCStressTest
@Outcome(id = {"true, true, false", "true, false, true", "false, true, true"},
        expect = ACCEPTABLE, desc = "Ровно двое получили разрешение")
@Outcome(id = "true, true, true", expect = FORBIDDEN, desc = "Выдано больше разрешений, чем было")
@Outcome(expect = FORBIDDEN, desc = "Ложная неудача: разрешения были, но tryAcquire вернул false")
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
