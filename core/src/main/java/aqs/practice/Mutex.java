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

    private static final class Sync extends AbstractQueuedSynchronizer {
        // TODO: убрать и использовать getExclusiveQueuedThreads()/setExclusiveQueuedThreads()
        private Thread currentThread;

        @Override
        protected boolean tryAcquire(int arg) {
            boolean acquired = compareAndSetState(0, 1);
            if (acquired)
                currentThread = Thread.currentThread();
            return acquired;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (!Thread.currentThread().equals(currentThread))
                throw new IllegalMonitorStateException("This thread cannot release the lock");
            // Очень важно делать currentThread = null, перед setState(0). Потому что если сделать setState(0) в начале,
            // то перед тем как сделаеться currentThread = null, другой поток уже сможет получить доступ к локу, сделать
            // сет на currentThread, а потом этот метод выполнит currentThread = null, и тем самым какой-то поток будет
            // держать лок, но при этом currentThread будет null. Получиться что вызовы к currentThread будут получать
            // null, хотя какой-то поток уже держит lock. Соответственно произойдет deadlock.
            currentThread = null;
            setState(0);
            return true;
        }

        @Override
        protected boolean isHeldExclusively() {
            return currentThread != null;
        }
    }

    private final Sync sync = new Sync();

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

    /** Whether the lock is held by any thread. */
    public boolean isLocked() {
        return sync.currentThread != null;
    }

    /** Whether the current thread holds the lock. */
    public boolean isHeldByCurrentThread() {
        return Thread.currentThread().equals(sync.currentThread);
    }

    /** Approximate number of threads waiting in the queue (for tests and monitoring). */
    public int getQueueLength() {
        return sync.getQueueLength();
    }
}
