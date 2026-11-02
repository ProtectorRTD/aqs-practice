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

    /** Примерное число потоков в очереди ожидания. */
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
            acquire(1);
        }

        @Override
        public void acquireSemaphore(int permits) throws InterruptedException {
            acquireInterruptibly(permits);
        }
    }
}
