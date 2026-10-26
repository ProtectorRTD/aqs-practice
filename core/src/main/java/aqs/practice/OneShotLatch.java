package aqs.practice;

import java.util.concurrent.TimeUnit;

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

    // TODO: Sync extends AbstractQueuedSynchronizer

    /** Ждёт, пока защёлку не откроют. */
    public void await() throws InterruptedException {
        throw new NotImplementedYet();
    }

    /** Ждёт не дольше таймаута. @return {@code true}, если защёлка открыта, {@code false} по таймауту. */
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        throw new NotImplementedYet();
    }

    /** Открывает защёлку и будит всех ждущих. */
    public void signal() {
        throw new NotImplementedYet();
    }

    /** Открыта ли защёлка. */
    public boolean isSignalled() {
        throw new NotImplementedYet();
    }

    /** Примерное число потоков в очереди ожидания. */
    public int getQueueLength() {
        throw new NotImplementedYet();
    }
}
