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
 * One thread waits on the latch, another opens it. The classic lost-wakeup check:
 * signal() may arrive at any moment: before queueing, between queueing and park, after park.
 */
@JCStressTest(Mode.Termination)
@Outcome(id = "TERMINATED", expect = ACCEPTABLE, desc = "The awaiter woke up")
@Outcome(id = "STALE", expect = FORBIDDEN, desc = "Signal lost, the thread hung")
@State
public class LatchAwaitWakeup {

    private final OneShotLatch latch = new OneShotLatch();

    @Actor
    public void awaiter() throws InterruptedException {
        latch.await();
    }

    @Signal
    public void signal() {
        latch.signal();
    }
}
