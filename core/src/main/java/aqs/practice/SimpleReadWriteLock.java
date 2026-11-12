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
    private Sync sync = new Sync();

    @Override
    public Lock readLock() {
        return sync.getRl();
    }

    @Override
    public Lock writeLock() {
        return sync.getWl();
    }

    /** How many read holds are currently held. */
    public int getReadLockCount() {
        return sync.getReaderCount();
    }

    public boolean isWriteLocked() {
        return sync.getWriterCount() > 0;
    }

    public boolean isWriteLockedByCurrentThread() {
        return sync.isWriterHolded();
    }

    /** Approximate number of threads waiting in the queue (readers and writers together). */
    public int getQueueLength() {
        return sync.getQueueLength();
    }


    private static final class Sync extends AbstractQueuedSynchronizer {
        private final Lock rl;
        private final Lock wl;

        public Sync () {
            super();
            this.rl = new ReaderLock(this);
            this.wl = new WriterLock(this);
        }

        @Override
        protected boolean tryAcquire(int arg) {
            while (true) {
                if (isHeldExclusively()) {
                    int state = getState();
                    int writerCount = getWriterCount(state);
                    if (writerCount >= 1) {
                        return false;
                    }

                    if (compareAndSetState(state, incrementWriter(state))) {
                        setExclusiveOwnerThread(Thread.currentThread());
                        return true;
                    }
                }
                return false;
            }
        }

        @Override
        protected boolean tryRelease(int arg) {
            while (true) {
                if (isHeldExclusively()) {
                    int state = getState();
                    if (state == 0) {
                        throw new IllegalMonitorStateException();
                    }
                    if (compareAndSetState(state, decrementWriter(state))) {
                        setExclusiveOwnerThread(null);
                        return true;
                    }
                }
                throw new IllegalMonitorStateException();
            }
        }

        @Override
        protected int tryAcquireShared(int arg) {
            while (true) {
                int state = getState();
                if (getWriterCount(state) > 0) {
                    //blocked by writer
                    return -1;
                }
                if (hasQueuedPredecessors()) {
                    return -1;
                }
                if (compareAndSetState(state, incrementReader(state))) {
                    return 1;
                }
            }
        }

        @Override
        protected boolean tryReleaseShared(int arg) {
            while (true) {
                int state = getState();
                if (getReaderCount(state) <= 0) {
                    throw new IllegalMonitorStateException();
                }
                if (state == 0) {
                    throw new IllegalMonitorStateException();
                }
                int update = decrementReader(state);
                if (compareAndSetState(state, update)) {
                    return true;
                }
            }
        }

        private int incrementWriter(int value) {
            if (getWriterCount(value) >= 65535) {
                throw new Error();
            }
            return value + 1;
        }

        private int decrementWriter(int value) {
            int writerCount = getWriterCount(value);
            if (writerCount <= 0) {
                throw new IllegalArgumentException();
            }
            return value - 1;
        }

        private int getWriterCount() {
            return getWriterCount(getState());
        }

        private int getWriterCount(int value) {
            int lowerMask = (1 << 16) - 1;
            return value & lowerMask;
        }

        private int getReaderCount() {
            return getReaderCount(getState());
        }

        private int getReaderCount(int value) {
            int mask = (1 << 16) - 1;
            return (value >>> 16) & mask;
        }

        private int decrementReader(int value) {
            if (getReaderCount(value) <= 0) {
                throw new IllegalStateException();
            }
            return value - (1 << 16);
        }

        private int incrementReader(int value) {
            if (getReaderCount(value) >= 65535) {
                throw new Error();
            }
            int afterIncrement = value + (1 << 16);
            return afterIncrement;
        }

        public Lock getRl() {
            return rl;
        }

        public Lock getWl() {
            return wl;
        }

        @Override
        protected boolean isHeldExclusively() {
            if (getWriterCount() > 0) {
                if (Thread.currentThread() == getExclusiveOwnerThread()) {
                    return true;
                }
                return false;
            }
            if (getReaderCount() == 0) {
                return true;
            }
            return false;
        }

        private boolean isWriterHolded() {
            if (getWriterCount() > 0) {
                if (Thread.currentThread() == getExclusiveOwnerThread()) {
                    return true;
                }
                return false;
            }
            return false;
        }
    }

    public static class ReaderLock implements Lock {
        private final Sync sync;
        public ReaderLock(Sync sync) {
            this.sync = sync;
        }

        @Override
        public void lock() {
            sync.acquireShared(1);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }

        @Override
        public boolean tryLock() {
            int i = sync.tryAcquireShared(1);
            return i == 1;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            sync.tryAcquireSharedNanos(1, unit.toNanos(time));
            return true;
        }

        @Override
        public void unlock() {
            sync.releaseShared(1);
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    public static class WriterLock implements Lock {
        private final Sync sync;
        public WriterLock(Sync sync) {
            this.sync = sync;
        }

        @Override
        public void lock() {
            sync.acquire(1);
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
            sync.release(1);
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }
}
