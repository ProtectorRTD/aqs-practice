package aqs.practice;

import java.util.concurrent.TimeUnit;
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

    // TODO: Sync / FairSync / NonfairSync

    public SimpleReentrantLock() {
        this(false);
    }

    public SimpleReentrantLock(boolean fair) {
        throw new NotImplementedYet();
    }

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

    public int getHoldCount() {
        throw new NotImplementedYet();
    }

    public boolean isHeldByCurrentThread() {
        throw new NotImplementedYet();
    }

    public boolean isLocked() {
        throw new NotImplementedYet();
    }

    public boolean isFair() {
        throw new NotImplementedYet();
    }

    /** Примерное число потоков в очереди ожидания лока. */
    public int getQueueLength() {
        throw new NotImplementedYet();
    }
}
