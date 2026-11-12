package aqs.practice;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Level 4. Reentrant lock with fair and nonfair modes and conditions.
 *
 * <p>AQS mode: exclusive.
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>Everything from {@link Mutex} except non-reentrancy: the owner may acquire the lock again,
 *       and the lock is released only after the same number of {@code unlock()} calls.</li>
 *   <li>{@link #getHoldCount()} is how many times the current thread holds the lock (0 if it does not).</li>
 *   <li>{@code unlock()} by a non-owner throws {@link IllegalMonitorStateException}, state unchanged.</li>
 *   <li>{@code Condition.await()} releases the lock fully, even if it is held several times,
 *       and restores the previous hold count after waking up. This holds in <b>both</b> modes.</li>
 *   <li><b>Fair mode</b>: nobody overtakes the queue. In particular {@link #tryLock()} returns
 *       {@code false} if someone is waiting in the queue (a deliberate difference from the JDK).
 *       A reentrant acquire by the owner still succeeds, of course.</li>
 *   <li><b>Nonfair mode</b> (default): barging is allowed.</li>
 * </ul>
 */
public class SimpleReentrantLock implements Lock {

    // TODO: Sync / FairSync / NonfairSync

    public SimpleReentrantLock() {
        this(false);
    }

    public SimpleReentrantLock(boolean fair) {
        throw new NotImplementedYet();
    }

    @Override
    public void lock() {
        throw new NotImplementedYet();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new NotImplementedYet();
    }

    @Override
    public boolean tryLock() {
        throw new NotImplementedYet();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        throw new NotImplementedYet();
    }

    @Override
    public void unlock() {
        throw new NotImplementedYet();
    }

    @Override
    public Condition newCondition() {
        throw new NotImplementedYet();
    }

    public int getHoldCount() {
        throw new NotImplementedYet();
    }

    public boolean isHeldByCurrentThread() {
        throw new NotImplementedYet();
    }

    public boolean isLocked() {
        throw new NotImplementedYet();
    }

    public boolean isFair() {
        throw new NotImplementedYet();
    }

    /** Approximate number of threads waiting for the lock. */
    public int getQueueLength() {
        throw new NotImplementedYet();
    }
}
