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
 * Two waiters needing one permit each, one release(2). Both must get through:
 * checks that the wakeup is passed down the queue (shared-mode propagation).
 * The actor starts the second waiter itself (Termination mode allows a single actor).
 */
@JCStressTest(Mode.Termination)
@Outcome(id = "TERMINATED", expect = ACCEPTABLE, desc = "Both waiters got through")
@Outcome(id = "STALE", expect = FORBIDDEN, desc = "One of the waiters hung although there were enough permits")
@State
public class SemaphoreReleasePropagation {

    private final SimpleSemaphore semaphore = new SimpleSemaphore(0);

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
