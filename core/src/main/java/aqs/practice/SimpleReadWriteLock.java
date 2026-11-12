package aqs.practice;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Уровень 5. Упрощённый read-write лок на <b>одном</b> AQS.
 *
 * <p>Режимы AQS: shared (чтение) и exclusive (запись) одновременно.
 * Главная идея уровня: один {@code int state} делится на две половины —
 * например, старшие 16 бит под число читателей, младшие под признак писателя.
 *
 * <h2>Контракт</h2>
 * <ul>
 *   <li>Читателей может быть много одновременно; писатель — один и только когда нет читателей.</li>
 *   <li>Оба лока <b>невозвратные</b>. Ни повышения read→write, ни понижения write→read нет:
 *       поток с read-локом на {@code writeLock().tryLock()} получает {@code false},
 *       а писатель на {@code readLock().tryLock()} и {@code writeLock().tryLock()} — тоже {@code false}.</li>
 *   <li>{@code writeLock().unlock()} не владельцем — {@link IllegalMonitorStateException}.</li>
 *   <li>{@code readLock().unlock()}, когда read-лок никто не держит, — {@link IllegalMonitorStateException}.
 *       (Учитывать, какой именно поток держит read-лок, не нужно — только общее число.)</li>
 *   <li>Защита писателя от голодания: пока в очереди ждёт писатель, новый читатель не проходит —
 *       {@code readLock().tryLock()} возвращает {@code false}, даже если сейчас лок держат только читатели.</li>
 *   <li>{@code tryLock()} не даёт «ложных» неудач из-за проигранного CAS.</li>
 *   <li>{@code newCondition()} у обоих локов бросает {@link UnsupportedOperationException}.</li>
 *   <li>Всё, что писатель записал до {@code unlock()}, видно читателю после {@code lock()}.</li>
 *   <li>Больше 65535 одновременных читателей — {@link Error} (переполнение половины state).</li>
 * </ul>
 */
public class SimpleReadWriteLock implements ReadWriteLock {
    private Sync sync = new Sync();
    // TODO: один Sync extends AbstractQueuedSynchronizer и два вида Lock поверх него

    @Override
    public Lock readLock() {
        return sync.getRl();
    }

    @Override
    public Lock writeLock() {
        return sync.getWl();
    }

    /** Сколько read-захватов сейчас удерживается. */
    public int getReadLockCount() {
        return sync.getReaderCount();
    }

    public boolean isWriteLocked() {
        return sync.getWriterCount() > 0;
    }

    public boolean isWriteLockedByCurrentThread() {
        return sync.isWriterHolded();
    }

    /** Примерное число потоков в очереди ожидания (читатели и писатели вместе). */
    public int getQueueLength() {
        return sync.getQueueLength();
    }


    private static final class Sync extends AbstractQueuedSynchronizer {
        private final Lock rl;
        private final Lock wl;

        public Sync () {
            super();
            this.rl = new ReaderLock(this);
            this.wl = new WriterLock(this);
        }

        @Override
        protected boolean tryAcquire(int arg) {
            while (true) {
                if (isHeldExclusively()) {
                    int state = getState();
                    int writerCount = getWriterCount(state);
                    if (writerCount >= 1) {
                        return false;
                    }

                    if (compareAndSetState(state, incrementWriter(state))) {
                        setExclusiveOwnerThread(Thread.currentThread());
                        return true;
                    }
                }
                return false;
            }
        }

        @Override
        protected boolean tryRelease(int arg) {
            while (true) {
                if (isHeldExclusively()) {
                    int state = getState();
                    if (state == 0) {
                        throw new IllegalMonitorStateException();
                    }
                    if (compareAndSetState(state, decrementWriter(state))) {
                        setExclusiveOwnerThread(null);
                        return true;
                    }
                }
                throw new IllegalMonitorStateException();
            }
        }

        @Override
        protected int tryAcquireShared(int arg) {
            while (true) {
                int state = getState();
                if (getWriterCount(state) > 0) {
                    //blocked by writer
                    return -1;
                }
                if (hasQueuedPredecessors()) {
                    return -1;
                }
                if (compareAndSetState(state, incrementReader(state))) {
                    return 1;
                }
            }
        }

        @Override
        protected boolean tryReleaseShared(int arg) {
            while (true) {
                int state = getState();
                if (state == 0) {
                    throw new IllegalMonitorStateException();
                }
                int update = decrementReader(state);
                if (compareAndSetState(state, update)) {
                    return true;
                }
            }
        }

        private int incrementWriter(int value) {
            if (getWriterCount(value) >= 65535) {
                throw new Error();
            }
            return value + 1;
        }

        private int decrementWriter(int value) {
            int writerCount = getWriterCount(value);
            if (writerCount <= 0) {
                throw new IllegalArgumentException();
            }
            return value - 1;
        }

        private int getWriterCount() {
            return getWriterCount(getState());
        }

        private int getWriterCount(int value) {
            int lowerMask = (1 << 16) - 1;
            return value & lowerMask;
        }

        private int getReaderCount() {
            return getReaderCount(getState());
        }

        private int getReaderCount(int value) {
            int mask = (1 << 16) - 1;
            return (value >>> 16) & mask;
        }

        private int decrementReader(int value) {
            if (getReaderCount(value) <= 0) {
                throw new IllegalStateException();
            }
            return value - (1 << 16);
        }

        private int incrementReader(int value) {
            if (getReaderCount(value) >= 65535) {
                throw new Error();
            }
            int afterIncrement = value + (1 << 16);
            return afterIncrement;
        }

        public Lock getRl() {
            return rl;
        }

        public Lock getWl() {
            return wl;
        }

        @Override
        protected boolean isHeldExclusively() {
            if (getWriterCount() > 0) {
                if (Thread.currentThread() == getExclusiveOwnerThread()) {
                    return true;
                }
                return false;
            }
            if (getReaderCount() == 0) {
                return true;
            }
            return false;
        }

        private boolean isWriterHolded() {
            if (getWriterCount() > 0) {
                if (Thread.currentThread() == getExclusiveOwnerThread()) {
                    return true;
                }
                return false;
            }
            return false;
        }
    }

    public static class ReaderLock implements Lock {
        private final Sync sync;
        public ReaderLock(Sync sync) {
            this.sync = sync;
        }

        @Override
        public void lock() {
            sync.acquireShared(1);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }

        @Override
        public boolean tryLock() {
            int i = sync.tryAcquireShared(1);
            return i == 1;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            sync.tryAcquireNanos(1, unit.toNanos(time));
            return true;
        }

        @Override
        public void unlock() {
            sync.releaseShared(1);
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    public static class WriterLock implements Lock {
        private final Sync sync;
        public WriterLock(Sync sync) {
            this.sync = sync;
        }

        @Override
        public void lock() {
            sync.acquire(1);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }

        @Override
        public boolean tryLock() {
            return sync.tryAcquire(1);
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(time));
        }

        @Override
        public void unlock() {
            sync.release(1);
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }
}
