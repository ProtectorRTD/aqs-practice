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

    private static final class Sync extends AbstractQueuedSynchronizer {
        // TODO: убрать и использовать getExclusiveQueuedThreads()/setExclusiveQueuedThreads()
        private Thread currentThread;

        @Override
        protected boolean tryAcquire(int arg) {
            boolean acquired = compareAndSetState(0, 1);
            if (acquired)
                currentThread = Thread.currentThread();
            return acquired;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (!Thread.currentThread().equals(currentThread))
                throw new IllegalMonitorStateException("This thread cannot release the lock");
            // Очень важно делать currentThread = null, перед setState(0). Потому что если сделать setState(0) в начале,
            // то перед тем как сделаеться currentThread = null, другой поток уже сможет получить доступ к локу, сделать
            // сет на currentThread, а потом этот метод выполнит currentThread = null, и тем самым какой-то поток будет
            // держать лок, но при этом currentThread будет null. Получиться что вызовы к currentThread будут получать
            // null, хотя какой-то поток уже держит lock. Соответственно произойдет deadlock.
            currentThread = null;
            setState(0);
            return true;
        }

        @Override
        protected boolean isHeldExclusively() {
            return currentThread != null;
        }
    }

    private final Sync sync = new Sync();

    @Override
    public void lock() {
        sync.acquire(0);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(0);
    }

    @Override
    public boolean tryLock() {
        try {
            return sync.tryAcquireNanos(0, 0);
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(0, unit.toNanos(time));
    }

    @Override
    public void unlock() {
        sync.release(0);
    }

    @Override
    public Condition newCondition() {
        return sync.new ConditionObject();
    }

    /** Занят ли лок каким-либо потоком. */
    public boolean isLocked() {
        return sync.currentThread != null;
    }

    /** Держит ли лок текущий поток. */
    public boolean isHeldByCurrentThread() {
        return Thread.currentThread().equals(sync.currentThread);
    }

    /** Примерное число потоков в очереди ожидания (для тестов и мониторинга). */
    public int getQueueLength() {
        return sync.getQueueLength();
    }
}
