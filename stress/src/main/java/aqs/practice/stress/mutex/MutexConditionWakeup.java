package aqs.practice.stress.mutex;

import aqs.practice.Mutex;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Mode;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.Signal;
import org.openjdk.jcstress.annotations.State;

import java.util.concurrent.locks.Condition;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/**
 * One thread waits on a Condition, another sets a flag under the lock and calls signal.
 * If the signal is lost, the awaiter sleeps forever (STALE).
 */
@JCStressTest(Mode.Termination)
@Outcome(id = "TERMINATED", expect = ACCEPTABLE, desc = "The awaiter woke up")
@Outcome(id = "STALE", expect = FORBIDDEN, desc = "Signal lost, the thread hung")
@State
public class MutexConditionWakeup {

    private final Mutex lock = new Mutex();
    private final Condition ready = lock.newCondition();
    private boolean flag;

    @Actor
    public void awaiter() throws InterruptedException {
        lock.lock();
        try {
            while (!flag) {
                ready.await();
            }
        } finally {
            lock.unlock();
        }
    }

    @Signal
    public void signal() {
        lock.lock();
        try {
            flag = true;
            ready.signal();
        } finally {
            lock.unlock();
        }
    }
}
