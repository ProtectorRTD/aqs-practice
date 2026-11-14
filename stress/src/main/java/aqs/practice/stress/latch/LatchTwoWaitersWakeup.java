package aqs.practice.stress.latch;

import aqs.practice.OneShotLatch;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Mode;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.Signal;
import org.openjdk.jcstress.annotations.State;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/**
 * Two waiters, one signal(). jcstress Termination mode allows only one actor,
 * so the actor starts the second waiter itself. Catches broken passing of the wakeup
 * down the chain (propagation): one wakes up, the other hangs.
 */
@JCStressTest(Mode.Termination)
@Outcome(id = "TERMINATED", expect = ACCEPTABLE, desc = "Both waiters woke up")
@Outcome(id = "STALE", expect = FORBIDDEN, desc = "One of the waiters did not wake up")
@State
public class LatchTwoWaitersWakeup {

    private final OneShotLatch latch = new OneShotLatch();

    @Actor
    public void awaiters() throws InterruptedException {
        Thread second = new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        second.start();
        latch.await();
        second.join();
    }

    @Signal
    public void signal() {
        latch.signal();
    }
}
