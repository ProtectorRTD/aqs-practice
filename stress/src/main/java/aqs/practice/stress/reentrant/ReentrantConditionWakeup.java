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
 * Ждущий держит лок дважды и уходит в await. Сигнальщик должен суметь захватить лок
 * (значит await отпустил его полностью) и разбудить ждущего.
 */
@JCStressTest(Mode.Termination)
@Outcome(id = "TERMINATED", expect = ACCEPTABLE, desc = "Ждущий проснулся")
@Outcome(id = "STALE", expect = FORBIDDEN, desc = "Завис: сигнал потерян или await не отпустил лок полностью")
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
