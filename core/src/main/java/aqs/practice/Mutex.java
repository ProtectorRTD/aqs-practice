package aqs.practice;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Уровень 1. Невозвратный (non-reentrant) взаимоисключающий лок.
 *
 * <p>Режим AQS: exclusive.
 *
 * <h2>Контракт</h2>
 * <ul>
 *   <li>В каждый момент лок держит не больше одного потока.</li>
 *   <li>Лок невозвратный: если владелец снова вызывает {@link #tryLock()}, получает {@code false}
 *       (а {@link #lock()} в этой ситуации повиснет навсегда, и это ожидаемо).</li>
 *   <li>{@link #unlock()} из потока, который лок не держит, бросает
 *       {@link IllegalMonitorStateException}, и состояние лока при этом не меняется.</li>
 *   <li>{@link #tryLock()} возвращает {@code false} только если лок реально занят:
 *       «ложных» неудач из-за конкуренции быть не должно.</li>
 *   <li>Всё, что владелец записал до {@code unlock()}, видно следующему владельцу после {@code lock()}.</li>
 *   <li>{@link #newCondition()} работает: {@code await}/{@code signal} без лока бросают
 *       {@link IllegalMonitorStateException}.</li>
 *   <li>{@link #lockInterruptibly()} и {@link #tryLock(long, TimeUnit)} реагируют на прерывание
 *       ({@link InterruptedException}); таймаут возвращает {@code false}.</li>
 * </ul>
 *
 * <p>Подсказка по форме: публичный класс — тонкая обёртка, вся логика в приватном
 * {@code Sync extends AbstractQueuedSynchronizer}. Какие методы AQS переопределять, решайте сами.
 */
public class Mutex implements Lock {
    private final Sync sync = new Sync();

    @Override
    public void lock() {
        sync.lock();
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
        sync.unLock();
    }

    @Override
    public Condition newCondition() {
        return sync.createConditionObject();
    }

    /** Занят ли лок каким-либо потоком. */
    public boolean isLocked() {
        return sync.isLocked();
    }

    /** Держит ли лок текущий поток. */
    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /** Примерное число потоков в очереди ожидания (для тестов и мониторинга). */
    public int getQueueLength() {
        return sync.getQueueLength();
    }

    private static final class Sync extends AbstractQueuedSynchronizer {
        public void lock() {
            acquire(1);
        }

        public boolean unLock() {
            return release(1);
        }

        @Override
        protected boolean tryAcquire(int arg) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            setExclusiveOwnerThread(null);
            boolean cas = compareAndSetState(1, 0);
            if (!cas)
                throw new IllegalMonitorStateException();
            return true;
        }

        @Override
        protected boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        public ConditionObject createConditionObject() {
            return new ConditionObject();
        }

        public boolean isLocked() {
            return getState() == 1;
        }
    }
}
