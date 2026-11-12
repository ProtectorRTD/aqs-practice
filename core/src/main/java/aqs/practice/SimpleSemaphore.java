package aqs.practice;

import java.util.concurrent.TimeUnit;

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

    // TODO: Sync / FairSync / NonfairSync

    public SimpleSemaphore(int permits) {
        this(permits, false);
    }

    public SimpleSemaphore(int permits, boolean fair) {
        throw new NotImplementedYet();
    }

    public void acquire() throws InterruptedException {
        throw new NotImplementedYet();
    }

    public void acquire(int permits) throws InterruptedException {
        throw new NotImplementedYet();
    }

    public void acquireUninterruptibly() {
        throw new NotImplementedYet();
    }

    public boolean tryAcquire() {
        throw new NotImplementedYet();
    }

    public boolean tryAcquire(int permits) {
        throw new NotImplementedYet();
    }

    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        throw new NotImplementedYet();
    }

    public void release() {
        throw new NotImplementedYet();
    }

    public void release(int permits) {
        throw new NotImplementedYet();
    }

    public int availablePermits() {
        throw new NotImplementedYet();
    }

    public boolean isFair() {
        throw new NotImplementedYet();
    }

    /** Approximate number of threads waiting in the queue. */
    public int getQueueLength() {
        throw new NotImplementedYet();
    }
}
