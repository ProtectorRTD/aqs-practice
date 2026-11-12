package aqs.practice;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Level 1. Non-reentrant mutual exclusion lock.
 *
 * <p>AQS mode: exclusive.
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>At most one thread holds the lock at any moment.</li>
 *   <li>The lock is non-reentrant: if the owner calls {@link #tryLock()} again, it gets {@code false}
 *       (and {@link #lock()} in that situation hangs forever, which is expected).</li>
 *   <li>{@link #unlock()} from a thread that does not hold the lock throws
 *       {@link IllegalMonitorStateException} and leaves the lock state unchanged.</li>
 *   <li>{@link #tryLock()} returns {@code false} only if the lock is actually held:
 *       no "spurious" failures caused by contention.</li>
 *   <li>Everything the owner wrote before {@code unlock()} is visible to the next owner after {@code lock()}.</li>
 *   <li>{@link #newCondition()} works: {@code await}/{@code signal} without holding the lock throw
 *       {@link IllegalMonitorStateException}.</li>
 *   <li>{@link #lockInterruptibly()} and {@link #tryLock(long, TimeUnit)} respond to interruption
 *       ({@link InterruptedException}); a timeout returns {@code false}.</li>
 * </ul>
 *
 * <p>Hint on structure: the public class is a thin wrapper, all logic lives in a private
 * {@code Sync extends AbstractQueuedSynchronizer}. Which AQS methods to override is up to you.
 */
public class Mutex implements Lock {
    private final Sync sync = new Sync();

    @Override
    public void lock() {
        sync.lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    @Override
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(time));
    }

    @Override
    public void unlock() {
        sync.unLock();
    }

    @Override
    public Condition newCondition() {
        return sync.createConditionObject();
    }

    /** Whether the lock is held by any thread. */
    public boolean isLocked() {
        return sync.isLocked();
    }

    /** Whether the current thread holds the lock. */
    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /** Approximate number of threads waiting in the queue (for tests and monitoring). */
    public int getQueueLength() {
        return sync.getQueueLength();
    }

    private static final class Sync extends AbstractQueuedSynchronizer {
        public void lock() {
            acquire(1);
        }

        public boolean unLock() {
            return release(1);
        }

        @Override
        protected boolean tryAcquire(int arg) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            setExclusiveOwnerThread(null);
            boolean cas = compareAndSetState(1, 0);
            if (!cas)
                throw new IllegalMonitorStateException();
            return true;
        }

        @Override
        protected boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        public ConditionObject createConditionObject() {
            return new ConditionObject();
        }

        public boolean isLocked() {
            return getState() == 1;
        }
    }
}
