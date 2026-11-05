package aqs.practice;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * Уровень 3. Счётный семафор с честным и нечестным режимом.
 *
 * <p>Режим AQS: shared.
 *
 * <h2>Контракт</h2>
 * <ul>
 *   <li>Конструктор с отрицательным числом разрешений бросает {@link IllegalArgumentException}.
 *       Отрицательный аргумент в {@code acquire(n)}/{@code tryAcquire(n)}/{@code release(n)} тоже.</li>
 *   <li>Число выданных одновременно разрешений никогда не превышает доступного.</li>
 *   <li>{@code tryAcquire} возвращает {@code false} только если разрешений реально не хватает
 *       (или, в честном режиме, есть очередь) — не из-за проигранного CAS.</li>
 *   <li>{@code release(n)} может будить сразу несколько ждущих, если им хватает разрешений.</li>
 *   <li>Владельца у разрешений нет: отпустить может любой поток, даже тот, кто ничего не брал.</li>
 *   <li><b>Честный режим</b>: никто не обгоняет очередь. В том числе {@link #tryAcquire()} возвращает
 *       {@code false}, если в очереди уже кто-то ждёт, даже когда свободных разрешений хватает.
 *       (Это сознательно отличается от JDK, где {@code tryAcquire()} обгоняет очередь всегда.)</li>
 *   <li><b>Нечестный режим</b> (по умолчанию): {@code tryAcquire} берёт разрешения, если их хватает,
 *       независимо от очереди.</li>
 *   <li>Всё, что поток записал до {@code release()}, видно потоку, который после этого сделал {@code acquire()}.</li>
 * </ul>
 *
 * <p>Подсказка по форме: см., как {@code ReentrantLock} разделяет fair/nonfair на два подкласса AQS.
 */
// TODO: разнциа между release/releaseShared
public class SimpleSemaphore {

    private static abstract class Sync extends AbstractQueuedSynchronizer {

        @Override
        protected boolean tryRelease(int arg) {
            if (arg < 0)
                throw new IllegalArgumentException("Permits cannot be < 0");
            int state = getState();
            return compareAndSetState(state, state + arg);
        }

        @Override
        protected abstract int tryAcquireShared(int arg);

        @Override
        protected boolean tryReleaseShared(int arg) {
            if (arg < 0)
                throw new IllegalArgumentException("Permits cannot be < 0");
            while (true) {
                int state = getState();
                if (compareAndSetState(state, state + arg))
                    break;
            }
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
            if (!isQueued(Thread.currentThread()))
                return -1;
            int state = getState();
            if (state - arg < 0)
                return -1;
            return compareAndSetState(state, state - arg) ? 1 : -1;
        }
    }

    private static final class UnfairSync extends Sync {

        @Override
        protected int tryAcquireShared(int arg) {
            if (arg < 0)
                throw new IllegalArgumentException("Permits cannot be < 0");
            int state = getState();
            if (state - arg < 0)
                return -1;
            return compareAndSetState(state, state - arg) ? 1 : -1;
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
        sync.releaseShared(1);
    }

    public void release(int permits) {
        sync.releaseShared(permits);
    }

    public int availablePermits() {
        return sync.getSyncState();
    }

    public boolean isFair() {
        return sync instanceof FairSync;
    }

    /** Примерное число потоков в очереди ожидания. */
    public int getQueueLength() {
        return sync.getQueueLength();
    }
}
