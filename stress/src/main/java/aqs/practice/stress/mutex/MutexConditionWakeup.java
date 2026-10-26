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
 * Поток ждёт на Condition, другой под локом выставляет флаг и делает signal.
 * Если сигнал теряется, ждущий спит вечно (STALE).
 */
@JCStressTest(Mode.Termination)
@Outcome(id = "TERMINATED", expect = ACCEPTABLE, desc = "Ждущий проснулся")
@Outcome(id = "STALE", expect = FORBIDDEN, desc = "Сигнал потерян, поток завис")
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
