package aqs.practice.stress.semaphore;

import aqs.practice.SimpleSemaphore;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/** A one-permit semaphore as a mutex: no increments are lost and the permit comes back. */
@JCStressTest
@Outcome(id = "2, 1", expect = ACCEPTABLE, desc = "Both increments are there, the permit came back")
@Outcome(expect = FORBIDDEN, desc = "An increment or a permit was lost")
@State
public class SemaphoreAsMutex {

    private final SimpleSemaphore semaphore = new SimpleSemaphore(1);
    private int x;

    @Actor
    public void actor1() {
        semaphore.acquireUninterruptibly();
        try {
            x++;
        } finally {
            semaphore.release();
        }
    }

    @Actor
    public void actor2() {
        semaphore.acquireUninterruptibly();
        try {
            x++;
        } finally {
            semaphore.release();
        }
    }

    @Arbiter
    public void arbiter(II_Result r) {
        r.r1 = x;
        r.r2 = semaphore.availablePermits();
    }
}
