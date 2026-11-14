package aqs.practice;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Level 5. Simplified read-write lock on a <b>single</b> AQS.
 *
 * <p>AQS modes: shared (read) and exclusive (write) at the same time.
 * The key idea of this level: one {@code int state} is split into two halves,
 * e.g. the upper 16 bits for the reader count and the lower ones for the writer flag.
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>Many readers may hold the lock at once; a writer holds it alone and only when there are no readers.</li>
 *   <li>Both locks are <b>non-reentrant</b>. There is no read→write upgrade and no write→read downgrade:
 *       a thread holding the read lock gets {@code false} from {@code writeLock().tryLock()},
 *       and the writer gets {@code false} from both {@code readLock().tryLock()} and {@code writeLock().tryLock()}.</li>
 *   <li>{@code writeLock().unlock()} by a non-owner throws {@link IllegalMonitorStateException}.</li>
 *   <li>{@code readLock().unlock()} when nobody holds the read lock throws {@link IllegalMonitorStateException}
 *       (including when the write lock is held).
 *       There is no need to track which thread holds the read lock, only the total count.</li>
 *   <li>Writer starvation protection: while a writer waits in the queue, a new reader does not get in —
 *       {@code readLock().tryLock()} returns {@code false} even if only readers hold the lock right now.</li>
 *   <li>{@code tryLock()} gives no "spurious" failures caused by a lost CAS.</li>
 *   <li>{@code readLock().tryLock(long, TimeUnit)} waits for a <b>read</b> hold and returns
 *       {@code false} on timeout.</li>
 *   <li>{@code newCondition()} on both locks throws {@link UnsupportedOperationException}.</li>
 *   <li>Everything the writer wrote before {@code unlock()} is visible to a reader after {@code lock()}.</li>
 *   <li>More than 65535 concurrent readers throws {@link Error} (overflow of the state half).</li>
 * </ul>
 */
public class SimpleReadWriteLock implements ReadWriteLock {
    private static final int WRITER_FLAG = 65536;
    private static final int MAX_READERS_COUNT = 65535;

    private final static class Sync extends AbstractQueuedSynchronizer {

        @Override
        protected int tryAcquireShared(int arg) {
            int state = getState();
            if (state == MAX_READERS_COUNT)
                throw new Error("Overflow of the state.");
            if (state >= WRITER_FLAG)
                return -1;
            if (!getExclusiveQueuedThreads().isEmpty() && hasQueuedPredecessors())
                return -1;
            return compareAndSetState(state, state + 1) ? 1 : -1;
        }

        @Override
        protected boolean tryAcquire(int arg) {
            if (compareAndSetState(0, WRITER_FLAG)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (!Thread.currentThread().equals(getExclusiveOwnerThread()))
                throw new IllegalMonitorStateException("Not a lock owner.");
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        @Override
        protected boolean tryReleaseShared(int arg) {
            if (getExclusiveOwnerThread() != null)
                throw new IllegalMonitorStateException("Writer holds the lock.");
            while (true) {
                int state = getState();
                if (state == 0)
                    throw new IllegalMonitorStateException("No readers hold the lock.");
                if (compareAndSetState(state, state - 1))
                    break;
            }
            return true;
        }

        public int getReadLockCount() {
            if (getState() >= WRITER_FLAG)
                return 0;
            return getState();
        }

        public Thread getWriteLockOwnerThread() {
            return getExclusiveOwnerThread();
        }
    }

    private record ReadLock(Sync sync) implements Lock {
        @Override
        public void lock() {
            sync.acquireShared(0);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireSharedInterruptibly(0);
        }

        @Override
        public boolean tryLock() {
            try {
                return sync.tryAcquireSharedNanos(0, 0);
            } catch (InterruptedException e) {
                return false;
            }
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireSharedNanos(0, unit.toNanos(time));
        }

        @Override
        public void unlock() {
            sync.releaseShared(0);
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("Condition is not supported.");
        }
    }

    private record WriteLock(Sync sync) implements Lock {

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
            sync.release(1);
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("Condition is not supported.");
        }
    }

    private final Sync sync = new Sync();
    private final ReadLock readLock = new ReadLock(sync);
    private final WriteLock writeLock = new WriteLock(sync);

    @Override
    public Lock readLock() {
        return readLock;
    }

    @Override
    public Lock writeLock() {
        return writeLock;
    }

    /** How many read holds are currently held. */
    public int getReadLockCount() {
        return sync.getReadLockCount();
    }

    public boolean isWriteLocked() {
        return sync.getWriteLockOwnerThread() != null;
    }

    public boolean isWriteLockedByCurrentThread() {
        return Thread.currentThread().equals(sync.getWriteLockOwnerThread());
    }

    /** Approximate number of threads waiting in the queue (readers and writers together). */
    public int getQueueLength() {
        return sync.getQueueLength();
    }
}
