package aqs.practice.stress.reentrant;

import aqs.practice.SimpleReentrantLock;
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
 * Same as {@link ReentrantConditionWakeup}, but for fair mode.
 * After waking up, await reacquires the lock with the saved hold count ({@code acquire(savedState)}),
 * so the fair {@code tryAcquire} must honour its argument. If it does not, the awaiter comes back
 * with a hold count of 1, and its second unlock throws {@link IllegalMonitorStateException}
 * (jcstress reports it as an error).
 */
@JCStressTest(Mode.Termination)
@Outcome(id = "TERMINATED", expect = ACCEPTABLE, desc = "The awaiter woke up")
@Outcome(id = "STALE", expect = FORBIDDEN, desc = "Hung: the signal was lost or await did not release the lock fully")
@State
public class FairReentrantConditionWakeup {

    private final SimpleReentrantLock lock = new SimpleReentrantLock(true);
    private final Condition ready = lock.newCondition();
    private boolean flag;

    @Actor
    public void awaiter() throws InterruptedException {
        lock.lock();
        lock.lock();
        try {
            while (!flag) {
                ready.await();
            }
        } finally {
            lock.unlock();
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
