package aqs.practice;

import java.util.concurrent.TimeUnit;

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

    // TODO: Sync extends AbstractQueuedSynchronizer

    /** Waits until the latch is opened. */
    public void await() throws InterruptedException {
        throw new NotImplementedYet();
    }

    /** Waits no longer than the timeout. @return {@code true} if the latch is open, {@code false} on timeout. */
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        throw new NotImplementedYet();
    }

    /** Opens the latch and wakes all waiters. */
    public void signal() {
        throw new NotImplementedYet();
    }

    /** Whether the latch is open. */
    public boolean isSignalled() {
        throw new NotImplementedYet();
    }

    /** Approximate number of threads waiting in the queue. */
    public int getQueueLength() {
        throw new NotImplementedYet();
    }
}
