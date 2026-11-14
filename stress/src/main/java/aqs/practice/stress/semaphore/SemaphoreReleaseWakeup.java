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

/** acquire() on an empty semaphore must wake up after release() from another thread. */
@JCStressTest(Mode.Termination)
@Outcome(id = "TERMINATED", expect = ACCEPTABLE, desc = "The awaiter woke up")
@Outcome(id = "STALE", expect = FORBIDDEN, desc = "Signal lost, the thread hung")
@State
public class SemaphoreReleaseWakeup {

    private final SimpleSemaphore semaphore = new SimpleSemaphore(0);

    @Actor
    public void acquirer() throws InterruptedException {
        semaphore.acquire();
    }

    @Signal
    public void signal() {
        semaphore.release();
    }
}
