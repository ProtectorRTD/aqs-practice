package aqs.practice;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
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

    private abstract static class Sync extends AbstractQueuedSynchronizer {
        protected int rememberedPermits;
        protected Thread rememeberedThread;

        @Override
        protected abstract boolean tryAcquire(int arg);

        @Override
        protected boolean tryRelease(int arg) {
            if (arg > 0) {
                rememberedPermits = arg;
                rememeberedThread = Thread.currentThread();
                setState(0);
                setExclusiveOwnerThread(null);
                return true;
            }
            if (!Thread.currentThread().equals(getExclusiveOwnerThread()))
                throw new IllegalMonitorStateException("This thread is not maintaining a lock.");
            // почему порядок тут влияет? Если сначала setState, а потом проверка if (getState() == 0), то есть вероятность дедлока
            if (getState() == 1)
                setExclusiveOwnerThread(null);
            setState(getState() - 1);
            return true;
        }

        @Override
        protected boolean isHeldExclusively() {
            return getExclusiveOwnerThread() != null;
        }

        protected Thread getCurrentThread() {
            return getExclusiveOwnerThread();
        }

        protected int getSyncState() {
            return getState();
        }

        protected boolean commonAcquire() {
            if (getExclusiveOwnerThread() == null) {
                boolean res = compareAndSetState(0, 1);
                if (!res)
                    return false;
                if (rememberedPermits > 0 && Thread.currentThread().equals(rememeberedThread)) {
                    setState(rememberedPermits);
                    rememberedPermits = 0;
                    rememeberedThread = null;
                }
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            if (Thread.currentThread().equals(getExclusiveOwnerThread())) {
                setState(getState() + 1);
                return true;
            }
            return false;
        }
    }

    private static final class FairSync extends Sync {

        @Override
        protected boolean tryAcquire(int arg) {
            if (hasQueuedThreads() && !Thread.currentThread().equals(getExclusiveOwnerThread()) && !Thread.currentThread().equals(getFirstQueuedThread()))
                return false;
            return commonAcquire();
        }
    }

    private static final class UnfairSync extends Sync {

        @Override
        protected boolean tryAcquire(int arg) {
            return commonAcquire();
        }
    }

    private final Sync sync;

    public SimpleReentrantLock() {
        this(false);
    }

    public SimpleReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new UnfairSync();
    }

    @Override
    public void lock() {
        sync.acquire(0);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(0);
    }

    @Override
    public boolean tryLock() {
        try {
            return sync.tryAcquireNanos(0, 0);
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(0, unit.toNanos(time));
    }

    @Override
    public void unlock() {
        sync.release(0);
    }

    @Override
    public Condition newCondition() {
        return sync.new ConditionObject();
    }

    public int getHoldCount() {
        if (!Thread.currentThread().equals(sync.getCurrentThread()))
            return 0;
        return sync.getSyncState();
    }

    public boolean isHeldByCurrentThread() {
        return Thread.currentThread().equals(sync.getCurrentThread());
    }

    public boolean isLocked() {
        return sync.getCurrentThread() != null;
    }

    public boolean isFair() {
        return sync instanceof FairSync;
    }

    /** Approximate number of threads waiting for the lock. */
    public int getQueueLength() {
        return sync.getQueueLength();
    }
}
