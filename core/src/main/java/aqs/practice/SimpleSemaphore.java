package aqs.practice;

import java.util.concurrent.TimeUnit;

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

    // TODO: Sync / FairSync / NonfairSync

    public SimpleSemaphore(int permits) {
        this(permits, false);
    }

    public SimpleSemaphore(int permits, boolean fair) {
        throw new NotImplementedYet();
    }

    public void acquire() throws InterruptedException {
        throw new NotImplementedYet();
    }

    public void acquire(int permits) throws InterruptedException {
        throw new NotImplementedYet();
    }

    public void acquireUninterruptibly() {
        throw new NotImplementedYet();
    }

    public boolean tryAcquire() {
        throw new NotImplementedYet();
    }

    public boolean tryAcquire(int permits) {
        throw new NotImplementedYet();
    }

    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        throw new NotImplementedYet();
    }

    public void release() {
        throw new NotImplementedYet();
    }

    public void release(int permits) {
        throw new NotImplementedYet();
    }

    public int availablePermits() {
        throw new NotImplementedYet();
    }

    public boolean isFair() {
        throw new NotImplementedYet();
    }

    /** Примерное число потоков в очереди ожидания. */
    public int getQueueLength() {
        throw new NotImplementedYet();
    }
}
