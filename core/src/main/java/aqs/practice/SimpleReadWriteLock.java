package aqs.practice;

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

    // TODO: one Sync extends AbstractQueuedSynchronizer and two kinds of Lock on top of it

    @Override
    public Lock readLock() {
        throw new NotImplementedYet();
    }

    @Override
    public Lock writeLock() {
        throw new NotImplementedYet();
    }

    /** How many read holds are currently held. */
    public int getReadLockCount() {
        throw new NotImplementedYet();
    }

    public boolean isWriteLocked() {
        throw new NotImplementedYet();
    }

    public boolean isWriteLockedByCurrentThread() {
        throw new NotImplementedYet();
    }

    /** Approximate number of threads waiting in the queue (readers and writers together). */
    public int getQueueLength() {
        throw new NotImplementedYet();
    }
}
