package aqs.practice;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Уровень 4. Возвратный (reentrant) лок с честным и нечестным режимом и условиями.
 *
 * <p>Режим AQS: exclusive.
 *
 * <h2>Контракт</h2>
 * <ul>
 *   <li>Всё из {@link Mutex}, кроме невозвратности: владелец может захватывать лок повторно,
 *       и лок освобождается только после такого же числа {@code unlock()}.</li>
 *   <li>{@link #getHoldCount()} — сколько раз текущий поток держит лок (0, если не держит).</li>
 *   <li>{@code unlock()} не владельцем — {@link IllegalMonitorStateException}, состояние не меняется.</li>
 *   <li>{@code Condition.await()} полностью отпускает лок, даже если он захвачен несколько раз,
 *       а после пробуждения восстанавливает прежний hold count.</li>
 *   <li><b>Честный режим</b>: никто не обгоняет очередь. В том числе {@link #tryLock()} возвращает
 *       {@code false}, если в очереди кто-то ждёт (сознательное отличие от JDK).
 *       Повторный захват владельцем при этом, конечно, проходит.</li>
 *   <li><b>Нечестный режим</b> (по умолчанию): разрешён barging.</li>
 * </ul>
 */
public class SimpleReentrantLock implements Lock {
    private final boolean fair;
    private final Sync sync;

    public SimpleReentrantLock() {
        this(false);
    }

    public SimpleReentrantLock(boolean fair) {
        this.fair = fair;
        if (!fair) {
            sync = new NonfairSync();
        } else {
            sync = new FairSync();
        }
    }

    @Override
    public void lock() {
        sync.lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.lockInterruptibly();
    }

    @Override
    public boolean tryLock() {
        return sync.tryLock();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryLock(time, unit);
    }

    @Override
    public void unlock() {
        sync.unlock();
    }

    @Override
    public Condition newCondition() {
        return sync.getConditionObject();
    }

    public int getHoldCount() {
        return sync.getHoldCount();
    }

    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    public boolean isLocked() {
        return sync.isLocked();
    }

    public boolean isFair() {
        return fair;
    }

    /**
     * Примерное число потоков в очереди ожидания лока.
     */
    public int getQueueLength() {
        return sync.getQueueLength();
    }

    private abstract class Sync extends AbstractQueuedSynchronizer {
        public void lock() {
            acquire(1);
        }

        public void lockInterruptibly() throws InterruptedException {
            acquireInterruptibly(1);
        }

        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return tryAcquireNanos(1, unit.toNanos(time));
        }

        public void unlock() {
            release(1);
        }

        public int getHoldCount() {
            if (isHeldExclusively())
                return getState();
            else
                return 0;
        }

        public boolean isLocked() {
            return getState() > 0;
        }

        public boolean tryLock() {
            return this.tryAcquire(1);
        }

        public ConditionObject getConditionObject() {
            return new ConditionObject();
        }

        @Override
        protected boolean isHeldExclusively() {
            Thread exclusiveOwnerThread = getExclusiveOwnerThread();
            Thread thread = Thread.currentThread();
            return exclusiveOwnerThread == thread;
        }
    }

    private class FairSync extends Sync {
        public FairSync() {
        }

        @Override
        protected boolean tryAcquire(int arg) {
            if (!isHeldExclusively() && hasQueuedPredecessors()) {
                return false;
            }
            //locked
            if (isLocked()) {
                if (isHeldExclusively()) {
                    int state = getState();
                    setState(state + arg);
                    return true;
                }
                return false;
            } else {
                //notLocked
                if (compareAndSetState(0, 1)) {
                    setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                }
                return false;
            }
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (isHeldExclusively()) {
                int state = getState();
                int updated = state - arg;
                if (updated == 0) {
                    setExclusiveOwnerThread(null);
                    setState(0);
                    return true;
                }
                setState(updated);
                return false;
            }
            throw new IllegalMonitorStateException();
        }
    }

    private class NonfairSync extends Sync {
        public NonfairSync() {
        }

        @Override
        protected boolean tryAcquire(int arg) {
            while (true) {
                if (compareAndSetState(0, arg)) {
                    setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                }
                //locked
                if (isLocked()) {
                    if (isHeldExclusively()) {
                        int state = getState();
                        if (compareAndSetState(state, state+arg)) {
                            return true;
                        }
                    } else {
                        return false;
                    }
                }
            }
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (isHeldExclusively()) {
                while (true) {
                    int state = getState();
                    int updated = state - arg;
                    if (updated == 0)
                        setExclusiveOwnerThread(null);
                    if (compareAndSetState(state, updated)) {
                        if (updated == 0) {
                            return true;
                        }
                        return false;
                    }
                }
            }
            throw new IllegalMonitorStateException();
        }
    }
}
