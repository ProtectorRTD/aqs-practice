package aqs.practice;

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

    // TODO: один Sync extends AbstractQueuedSynchronizer и два вида Lock поверх него

    @Override
    public Lock readLock() {
        throw new NotImplementedYet();
    }

    @Override
    public Lock writeLock() {
        throw new NotImplementedYet();
    }

    /** Сколько read-захватов сейчас удерживается. */
    public int getReadLockCount() {
        throw new NotImplementedYet();
    }

    public boolean isWriteLocked() {
        throw new NotImplementedYet();
    }

    public boolean isWriteLockedByCurrentThread() {
        throw new NotImplementedYet();
    }

    /** Примерное число потоков в очереди ожидания (читатели и писатели вместе). */
    public int getQueueLength() {
        throw new NotImplementedYet();
    }
}
