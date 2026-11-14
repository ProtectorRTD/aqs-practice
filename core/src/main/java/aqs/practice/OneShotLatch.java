package aqs.practice;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * Level 2. One-shot latch: closed until someone calls {@link #signal()}, open forever after that.
 * Essentially {@code CountDownLatch(1)}.
 *
 * <p>AQS mode: shared.
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>Before {@code signal()}, every {@link #await()} blocks.</li>
 *   <li>{@code signal()} wakes <b>all</b> waiters, not just one.</li>
 *   <li>After {@code signal()}, any {@code await()} returns immediately. The latch never closes again.</li>
 *   <li>A repeated {@code signal()} is safe and changes nothing.</li>
 *   <li>Everything a thread wrote before {@code signal()} is visible to a thread after it returns from {@code await()}.</li>
 *   <li>{@link #await()} is interruptible; {@link #await(long, TimeUnit)} returns {@code false} on timeout.</li>
 *   <li>Any thread may call {@code signal()}; there is no owner.</li>
 * </ul>
 *
 * <p>A question to ask yourself before implementing: what does "acquire" mean here if nobody takes anything?
 */
public class OneShotLatch {

    // TODO: разобрать как работает tryAcquireShared и как он помогает разбудеить все потоки
    private static final class Sync extends AbstractQueuedSynchronizer {

        @Override
        protected int tryAcquireShared(int arg) {
            if (getState() == 0)
                return -1;
            return 1;
        }

        @Override
        protected boolean tryRelease(int arg) {
            setState(1);
            return true;
        }

        public int getSyncState() {
            return getState();
        }
    }

    private final Sync sync = new Sync();

    /** Waits until the latch is opened. */
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(0);
    }

    /** Waits no longer than the timeout. @return {@code true} if the latch is open, {@code false} on timeout. */
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireSharedNanos(0, unit.toNanos(timeout));
    }

    /** Opens the latch and wakes all waiters. */
    public void signal() {
        sync.release(0);

    }

    /** Whether the latch is open. */
    public boolean isSignalled() {
        return sync.getSyncState() == 1;
    }

    /** Approximate number of threads waiting in the queue. */
    public int getQueueLength() {
        return sync.getQueueLength();
    }
}
