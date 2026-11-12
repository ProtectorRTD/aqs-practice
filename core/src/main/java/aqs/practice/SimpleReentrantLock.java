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
    private final boolean fair;
    private final Sync sync;

    public SimpleReentrantLock() {
        this(false);
    }

    public SimpleReentrantLock(boolean fair) {
        this.fair = fair;
        if (!fair) {
            sync = new NonfairSync();
        } else {
            sync = new FairSync();
        }
    }

    @Override
    public void lock() {
        sync.lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.lockInterruptibly();
    }

    @Override
    public boolean tryLock() {
        return sync.tryLock();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryLock(time, unit);
    }

    @Override
    public void unlock() {
        sync.unlock();
    }

    @Override
    public Condition newCondition() {
        return sync.getConditionObject();
    }

    public int getHoldCount() {
        return sync.getHoldCount();
    }

    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    public boolean isLocked() {
        return sync.isLocked();
    }

    public boolean isFair() {
        return fair;
    }

    /** Approximate number of threads waiting for the lock. */
    public int getQueueLength() {
        return sync.getQueueLength();
    }

    private abstract class Sync extends AbstractQueuedSynchronizer {
        public void lock() {
            acquire(1);
        }

        public void lockInterruptibly() throws InterruptedException {
            acquireInterruptibly(1);
        }

        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return tryAcquireNanos(1, unit.toNanos(time));
        }

        public void unlock() {
            release(1);
        }

        public int getHoldCount() {
            if (isHeldExclusively())
                return getState();
            else
                return 0;
        }

        public boolean isLocked() {
            return getState() > 0;
        }

        public boolean tryLock() {
            return this.tryAcquire(1);
        }

        public ConditionObject getConditionObject() {
            return new ConditionObject();
        }

        @Override
        protected boolean isHeldExclusively() {
            Thread exclusiveOwnerThread = getExclusiveOwnerThread();
            Thread thread = Thread.currentThread();
            return exclusiveOwnerThread == thread;
        }
    }

    private class FairSync extends Sync {
        public FairSync() {
        }

        @Override
        protected boolean tryAcquire(int arg) {
            if (!isHeldExclusively() && hasQueuedPredecessors()) {
                return false;
            }
            //locked
            if (isLocked()) {
                if (isHeldExclusively()) {
                    int state = getState();
                    setState(state + arg);
                    return true;
                }
                return false;
            } else {
                //notLocked
                if (compareAndSetState(0, 1)) {
                    setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                }
                return false;
            }
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (isHeldExclusively()) {
                int state = getState();
                int updated = state - arg;
                if (updated == 0) {
                    setExclusiveOwnerThread(null);
                    setState(0);
                    return true;
                }
                setState(updated);
                return false;
            }
            throw new IllegalMonitorStateException();
        }
    }

    private class NonfairSync extends Sync {
        public NonfairSync() {
        }

        @Override
        protected boolean tryAcquire(int arg) {
            while (true) {
                if (compareAndSetState(0, arg)) {
                    setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                }
                //locked
                if (isLocked()) {
                    if (isHeldExclusively()) {
                        int state = getState();
                        if (compareAndSetState(state, state+arg)) {
                            return true;
                        }
                    } else {
                        return false;
                    }
                }
            }
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (isHeldExclusively()) {
                while (true) {
                    int state = getState();
                    int updated = state - arg;
                    if (updated == 0)
                        setExclusiveOwnerThread(null);
                    if (compareAndSetState(state, updated)) {
                        if (updated == 0) {
                            return true;
                        }
                        return false;
                    }
                }
            }
            throw new IllegalMonitorStateException();
        }
    }
}
