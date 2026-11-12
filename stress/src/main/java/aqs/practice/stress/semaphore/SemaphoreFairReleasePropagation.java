package aqs.practice.stress.semaphore;

import aqs.practice.SimpleSemaphore;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Mode;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.Signal;
import org.openjdk.jcstress.annotations.State;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/**
 * Same as {@link SemaphoreReleasePropagation}, but for fair mode.
 * Fairness only adds "do not overtake the queue"; the semaphore is still shared,
 * so one release(2) must wake both waiters. A fair semaphore built on exclusive
 * acquire/release wakes only the first one, and the second hangs.
 */
@JCStressTest(Mode.Termination)
@Outcome(id = "TERMINATED", expect = ACCEPTABLE, desc = "Both waiters got through")
@Outcome(id = "STALE", expect = FORBIDDEN, desc = "One of the waiters hung although there were enough permits")
@State
public class SemaphoreFairReleasePropagation {

    private final SimpleSemaphore semaphore = new SimpleSemaphore(0, true);

    @Actor
    public void acquirers() throws InterruptedException {
        Thread second = new Thread(semaphore::acquireUninterruptibly);
        second.start();
        semaphore.acquireUninterruptibly();
        second.join();
    }

    @Signal
    public void signal() {
        semaphore.release(2);
    }
}
