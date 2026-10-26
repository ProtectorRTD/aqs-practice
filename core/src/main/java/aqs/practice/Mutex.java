package aqs.practice;

import java.util.concurrent.TimeUnit;
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

    // TODO: private static final class Sync extends AbstractQueuedSynchronizer { ... }
    // TODO: private final Sync sync = new Sync();

    @Override
    public void lock() {
        throw new NotImplementedYet();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new NotImplementedYet();
    }

    @Override
    public boolean tryLock() {
        throw new NotImplementedYet();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        throw new NotImplementedYet();
    }

    @Override
    public void unlock() {
        throw new NotImplementedYet();
    }

    @Override
    public Condition newCondition() {
        throw new NotImplementedYet();
    }

    /** Занят ли лок каким-либо потоком. */
    public boolean isLocked() {
        throw new NotImplementedYet();
    }

    /** Держит ли лок текущий поток. */
    public boolean isHeldByCurrentThread() {
        throw new NotImplementedYet();
    }

    /** Примерное число потоков в очереди ожидания (для тестов и мониторинга). */
    public int getQueueLength() {
        throw new NotImplementedYet();
    }
}
