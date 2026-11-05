package aqs.practice;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * Уровень 2. Одноразовая защёлка: закрыта, пока кто-то не вызовет {@link #signal()}, дальше открыта навсегда.
 * По сути {@code CountDownLatch(1)}.
 *
 * <p>Режим AQS: shared.
 *
 * <h2>Контракт</h2>
 * <ul>
 *   <li>До {@code signal()} все {@link #await()} блокируются.</li>
 *   <li>{@code signal()} будит <b>всех</b> ждущих, а не одного.</li>
 *   <li>После {@code signal()} любой {@code await()} возвращается сразу. Обратно защёлка не закрывается.</li>
 *   <li>Повторный {@code signal()} безопасен и ничего не меняет.</li>
 *   <li>Всё, что поток записал до {@code signal()}, видно потоку после выхода из {@code await()}.</li>
 *   <li>{@link #await()} прерываемый; {@link #await(long, TimeUnit)} возвращает {@code false} по таймауту.</li>
 *   <li>{@code signal()} может вызвать любой поток, владельца нет.</li>
 * </ul>
 *
 * <p>Вопрос себе перед реализацией: что здесь означает «acquire», если никто ничего не захватывает?
 */
public class OneShotLatch {

    // TODO: разобрать как работает tryAcquireShared и как он помогает разбудеить все потоки
    private static final class Sync extends AbstractQueuedSynchronizer {

        @Override
        protected int tryAcquireShared(int arg) {
            if (getState() == 0)
                return -1;
            return 1;
        }

        @Override
        protected boolean tryRelease(int arg) {
            setState(1);
            return true;
        }

        public int getSyncState() {
            return getState();
        }
    }

    private final Sync sync = new Sync();

    /** Ждёт, пока защёлку не откроют. */
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(0);
    }

    /** Ждёт не дольше таймаута. @return {@code true}, если защёлка открыта, {@code false} по таймауту. */
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireSharedNanos(0, unit.toNanos(timeout));
    }

    /** Открывает защёлку и будит всех ждущих. */
    public void signal() {
        sync.release(0);

    }

    /** Открыта ли защёлка. */
    public boolean isSignalled() {
        return sync.getSyncState() == 1;
    }

    /** Примерное число потоков в очереди ожидания. */
    public int getQueueLength() {
        return sync.getQueueLength();
    }
}
