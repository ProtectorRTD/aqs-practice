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
 * The awaiter holds the lock twice and goes into await. The signaller must be able to acquire the lock
 * (so await released it fully) and wake the awaiter.
 */
@JCStressTest(Mode.Termination)
@Outcome(id = "TERMINATED", expect = ACCEPTABLE, desc = "The awaiter woke up")
@Outcome(id = "STALE", expect = FORBIDDEN, desc = "Hung: the signal was lost or await did not release the lock fully")
@State
public class ReentrantConditionWakeup {

    private final SimpleReentrantLock lock = new SimpleReentrantLock();
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
