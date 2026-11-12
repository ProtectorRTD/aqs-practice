package aqs.practice.stress.latch;

import aqs.practice.OneShotLatch;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.concurrent.TimeUnit;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/**
 * A write before signal() must be visible after await(): release/acquire in AQS give happens-before
 * through the volatile state. The plain (non-volatile) field here is intentional.
 */
@JCStressTest
@Outcome(id = "42", expect = ACCEPTABLE, desc = "Data published")
@Outcome(id = "-1", expect = FORBIDDEN, desc = "await did not get the signal(): the signal was lost")
@Outcome(expect = FORBIDDEN, desc = "The old value is visible after await()")
@State
public class LatchPublishesData {

    private final OneShotLatch latch = new OneShotLatch();
    private int data;

    @Actor
    public void publisher() {
        data = 42;
        latch.signal();
    }

    @Actor
    public void consumer(I_Result r) {
        // timed await: if the signal is occasionally lost, the test shows -1 instead of hanging the whole run.
        // (In jcstress continuous mode actors cannot throw checked exceptions.)
        try {
            r.r1 = latch.await(5, TimeUnit.SECONDS) ? data : -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            r.r1 = -2;
        }
    }
}
