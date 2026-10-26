package aqs.practice;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Держит лок в отдельном потоке, пока его не закроют.
 * Нужен, чтобы проверять поведение «лок занят кем-то другим».
 * Для координации используется JDK-шный CountDownLatch, а не ваш код.
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
            throw new AssertionError("holder не смог захватить лок", t);
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
