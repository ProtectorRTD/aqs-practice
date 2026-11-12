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
public class SimpleSemaphore {
    private Sync sync;
    private final boolean fair;

    public SimpleSemaphore(int permits) {
        this(permits, false);
    }

    public SimpleSemaphore(int permits, boolean fair) {
        if (permits < 0) {
            throw new IllegalArgumentException();
        }
        this.fair = fair;
        if (!fair) {
            sync = new NonfairSync(permits);
        } else {
            sync = new FairSync(permits);
        }
    }

    public void acquire() throws InterruptedException {
        sync.acquireSemaphore(1);
    }

    public void acquire(int permits) throws InterruptedException {
        if (permits < 0) {
            throw new IllegalArgumentException();
        }
        sync.acquireSemaphore(permits);
    }

    public void acquireUninterruptibly() {
        sync.acquireUninterruptiblySemaphore(1);
    }

    public boolean tryAcquire() {
        return sync.tryAcquireSemaphore(1);
    }

    public boolean tryAcquire(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException();
        }
        return sync.tryAcquireSemaphore(permits);
    }

    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireTimeoutSemaphore(timeout, unit);
    }

    public void release() {
        sync.releaseSemaphore(1);
    }

    public void release(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException();
        }
        sync.releaseSemaphore(permits);
    }

    public int availablePermits() {
        return sync.availablePermit();
    }

    public boolean isFair() {
        return this.fair;
    }

    /** Approximate number of threads waiting in the queue. */
    public int getQueueLength() {
        return sync.getQueueLength();
    }

    public abstract class Sync extends AbstractQueuedSynchronizer {
        protected int permits;

        public abstract void acquireSemaphore(int permits) throws InterruptedException;
        public abstract void acquireUninterruptiblySemaphore(int permits);
        public abstract void releaseSemaphore(int permits);
        public abstract boolean tryAcquireSemaphore(int permits);
        public abstract boolean tryAcquireTimeoutSemaphore(long timeout, TimeUnit unit) throws InterruptedException;

        public int availablePermit() {
            return permits - getState();
        }

        public Sync(int permits) {
            this.permits = permits;
        }

        @Override
        protected boolean isHeldExclusively() {
            return true;
        }
    }

    public class NonfairSync extends Sync {
        public NonfairSync(int permits) {
            super(permits);
        }

        @Override
        public boolean tryAcquireSemaphore(int arg) {
            while (true) {
                int state = getState();
                if (arg > (permits - state))
                    return false;
                if (compareAndSetState(state, state + arg)) {
                    return true;
                }
            }
        }

        @Override
        public void acquireSemaphore(int permits) throws InterruptedException {
            acquireSharedInterruptibly(permits);
        }

        @Override
        public void acquireUninterruptiblySemaphore(int permits) {
            acquireShared(1);
        }

        @Override
        public void releaseSemaphore(int permits) {
            releaseShared(permits);
        }

        @Override
        protected int tryAcquireShared(int arg) {
            while (true) {
                int state = getState();
                if (arg > (permits - state)) {
                    return -1;
                }
                if (compareAndSetState(state, state + arg))
                    return 1;
            }
        }

        @Override
        protected boolean tryReleaseShared(int arg) {
            while (true) {
                int state = getState();
                if (compareAndSetState(state, state - arg))
                    return true;
            }
        }


        @Override
        public boolean tryAcquireTimeoutSemaphore(long timeout, TimeUnit unit) throws InterruptedException {
            return tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }
    }

    public class FairSync extends Sync {
        public FairSync(int permits) {
            super(permits);
        }

        @Override
        protected boolean tryRelease(int arg) {
            while (true) {
                int state = getState();
                if (compareAndSetState(state, state - arg))
                    return true;
            }
        }

        @Override
        protected boolean tryAcquire(int arg) {
            while (true) {
                int state = getState();
                if (hasQueuedPredecessors() || arg > (permits - state))
                    return false;
                boolean cas = compareAndSetState(state, state + arg);
                if (cas) {
                    return true;
                }
            }
        }

        @Override
        public boolean tryAcquireTimeoutSemaphore(long timeout, TimeUnit unit) throws InterruptedException {
            return tryAcquireNanos(1, unit.toNanos(timeout));
        }

        @Override
        public boolean tryAcquireSemaphore(int arg) {
            while (true) {
                int state = getState();
                if (hasQueuedPredecessors() || arg > (permits - state))
                    return false;
                if (compareAndSetState(state, state + arg))
                    return true;
            }
        }

        @Override
        public void releaseSemaphore(int permits) {
            release(permits);
        }

        @Override
        public void acquireUninterruptiblySemaphore(int permits) {
            acquire(permits);
        }

        @Override
        public void acquireSemaphore(int permits) throws InterruptedException {
            acquireInterruptibly(permits);
        }
    }
}
