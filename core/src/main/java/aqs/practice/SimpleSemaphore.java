package aqs.practice;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * Level 3. Counting semaphore with fair and nonfair modes.
 *
 * <p>AQS mode: shared.
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>The constructor throws {@link IllegalArgumentException} for a negative number of permits.
 *       So does a negative argument to {@code acquire(n)}/{@code tryAcquire(n)}/{@code release(n)}.</li>
 *   <li>The number of permits handed out at once never exceeds what is available.</li>
 *   <li>{@code tryAcquire} returns {@code false} only if there really are not enough permits
 *       (or, in fair mode, there is a queue), never because of a lost CAS.</li>
 *   <li>{@code release(n)} may wake several waiters at once if there are enough permits for them.
 *       This holds in <b>both</b> modes.</li>
 *   <li>Permits have no owner: any thread may release, even one that never acquired anything.</li>
 *   <li><b>Fair mode</b>: nobody overtakes the queue. In particular {@link #tryAcquire()} returns
 *       {@code false} if someone is already waiting in the queue, even when there are enough free permits.
 *       (This deliberately differs from the JDK, where {@code tryAcquire()} always barges.)</li>
 *   <li><b>Nonfair mode</b> (default): {@code tryAcquire} takes permits if there are enough of them,
 *       regardless of the queue.</li>
 *   <li>Everything a thread wrote before {@code release()} is visible to a thread that called {@code acquire()} after it.</li>
 * </ul>
 *
 * <p>Hint on structure: see how {@code ReentrantLock} splits fair/nonfair into two AQS subclasses.
 */
// TODO: разнциа между release/releaseShared
public class SimpleSemaphore {

    private static abstract class Sync extends AbstractQueuedSynchronizer {

        @Override
        protected boolean tryRelease(int arg) {
            if (arg < 0)
                throw new IllegalArgumentException("Permits cannot be < 0");
            int state;
            do {
                state = getState();
            } while (!compareAndSetState(state, state + arg));
            return true;
        }

        @Override
        protected abstract int tryAcquireShared(int arg);

        @Override
        protected boolean tryReleaseShared(int arg) {
            if (arg < 0)
                throw new IllegalArgumentException("Permits cannot be < 0");
            int state;
            do {
                state = getState();
            } while (!compareAndSetState(state, state + arg));
            return true;
        }

        protected void setSyncState(int state) {
            setState(state);
        }

        protected int getSyncState() {
            return getState();
        }
    }

    private static final class FairSync extends Sync {

        @Override
        protected int tryAcquireShared(int arg) {
            if (arg < 0)
                throw new IllegalArgumentException("Permits cannot be < 0");
            if (!isQueued(Thread.currentThread()) && hasQueuedThreads())
                return -1;
            while (true) {
                int state = getState();
                if (state - arg < 0)
                    return -1;
                if (compareAndSetState(state, state - arg))
                    return 1;
            }
        }
    }

    private static final class UnfairSync extends Sync {

        @Override
        protected int tryAcquireShared(int arg) {
            if (arg < 0)
                throw new IllegalArgumentException("Permits cannot be < 0");
            while (true) {
                int state = getState();
                if (state - arg < 0)
                    return -1;
                if (compareAndSetState(state, state - arg))
                    return 1;
            }
        }
    }

    private final Sync sync;

    public SimpleSemaphore(int permits) {
        this(permits, false);
    }

    public SimpleSemaphore(int permits, boolean fair) {
        if (permits < 0)
            throw new IllegalArgumentException("Permits cannot be < 0");
        sync = fair ? new FairSync() : new UnfairSync();
        sync.setSyncState(permits);
    }

    public void acquire() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    public void acquire(int permits) throws InterruptedException {
        sync.acquireSharedInterruptibly(permits);
    }

    public void acquireUninterruptibly() {
        sync.acquireShared(1);
    }

    public boolean tryAcquire() {
        try {
            return sync.tryAcquireSharedNanos(1, 0);
        } catch (InterruptedException e) {
            return false;
        }
    }

    public boolean tryAcquire(int permits) {
        try {
            return sync.tryAcquireSharedNanos(permits, 0);
        } catch (InterruptedException e) {
            return false;
        }
    }

    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    public void release() {
        sync.release(1);
    }

    public void release(int permits) {
        sync.release(permits);
    }

    public int availablePermits() {
        return sync.getSyncState();
    }

    public boolean isFair() {
        return sync instanceof FairSync;
    }

    /** Approximate number of threads waiting in the queue. */
    public int getQueueLength() {
        return sync.getQueueLength();
    }
}
