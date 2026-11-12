package aqs.practice.stress.mutex;

import aqs.practice.Mutex;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/**
 * Two threads do {@code x++} under the lock. A lost increment means
 * the lock let two threads in at once.
 */
@JCStressTest
@Outcome(id = "2", expect = ACCEPTABLE, desc = "Both increments are there")
@Outcome(expect = FORBIDDEN, desc = "Increment lost: two threads were in the critical section at the same time")
@State
public class MutexMutualExclusion {

    private final Mutex lock = new Mutex();
    private int x;

    @Actor
    public void actor1() {
        lock.lock();
        try {
            x++;
        } finally {
            lock.unlock();
        }
    }

    @Actor
    public void actor2() {
        lock.lock();
        try {
            x++;
        } finally {
            lock.unlock();
        }
    }

    @Arbiter
    public void arbiter(I_Result r) {
        r.r1 = x;
    }
}
