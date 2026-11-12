package aqs.practice;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Holds a lock in a separate thread until closed.
 * Used to check behaviour when "the lock is held by someone else".
 * Coordination uses the JDK CountDownLatch, not your code.
 */
final class LockHolder implements AutoCloseable {

    private final CountDownLatch release = new CountDownLatch(1);
    private final TestThreads.Worker worker;

    private LockHolder(Lock lock) throws InterruptedException {
        CountDownLatch acquired = new CountDownLatch(1);
        worker = TestThreads.start("holder", () -> {
            lock.lock();
            try {
                acquired.countDown();
                release.await();
            } finally {
                lock.unlock();
            }
        });
        if (!acquired.await(TestThreads.TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            Throwable t = worker.error();
            throw new AssertionError("holder could not acquire the lock", t);
        }
    }

    static LockHolder hold(Lock lock) throws InterruptedException {
        return new LockHolder(lock);
    }

    @Override
    public void close() throws InterruptedException {
        release.countDown();
        worker.joinOk();
    }
}
