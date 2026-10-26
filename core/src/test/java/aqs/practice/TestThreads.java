package aqs.practice;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Утилиты для детерминированных многопоточных JUnit-тестов.
 * Никаких sleep «на удачу»: ждём наблюдаемого события (поток встал в очередь, поток завершился)
 * с жёстким таймаутом.
 */
final class TestThreads {

    static final long TIMEOUT_MS = 5_000;

    private TestThreads() {
    }

    @FunctionalInterface
    interface Body {
        void run() throws Throwable;
    }

    /** Поток, который запоминает исключение из тела, чтобы тест мог его проверить. */
    static final class Worker extends Thread {
        private final Body body;
        private final AtomicReference<Throwable> error = new AtomicReference<>();

        Worker(String name, Body body) {
            super(name);
            this.body = body;
            setDaemon(true); // зависший поток не держит JVM после падения теста
        }

        @Override
        public void run() {
            try {
                body.run();
            } catch (Throwable t) {
                error.set(t);
            }
        }

        /** Ждёт завершения; падает, если поток не завершился или бросил исключение. */
        void joinOk() throws InterruptedException {
            join(TIMEOUT_MS);
            if (isAlive()) {
                fail("Поток " + getName() + " не завершился за " + TIMEOUT_MS + " мс (потерянный сигнал?)");
            }
            Throwable t = error.get();
            if (t != null) {
                throw new AssertionError("Поток " + getName() + " бросил исключение", t);
            }
        }

        Throwable error() {
            return error.get();
        }
    }

    static Worker start(String name, Body body) {
        Worker w = new Worker(name, body);
        w.start();
        return w;
    }

    /** Ждёт, пока в очереди синхронизатора окажется не меньше {@code expected} потоков. */
    static void awaitQueueLength(IntSupplier queueLength, int expected) throws InterruptedException {
        awaitCondition(() -> queueLength.getAsInt() >= expected,
                "в очереди не появилось " + expected + " потоков");
    }

    static void awaitCondition(BooleanSupplier condition, String message) throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT_MS * 1_000_000L;
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("Не дождались за " + TIMEOUT_MS + " мс: " + message);
            }
            Thread.sleep(1);
        }
    }

    /** Ждёт, пока поток реально уснёт (park), а не просто встанет в очередь. */
    static void awaitParked(Thread t) throws InterruptedException {
        awaitCondition(() -> {
            Thread.State s = t.getState();
            return s == Thread.State.WAITING || s == Thread.State.TIMED_WAITING || s == Thread.State.TERMINATED;
        }, "поток " + t.getName() + " не уснул");
        if (!t.isAlive()) {
            fail("Поток " + t.getName() + " должен был заблокироваться, но завершился");
        }
    }

    /**
     * Проверяет, что поток всё ещё не прошёл дальше. Это «отрицательная» проверка,
     * поэтому она неизбежно с паузой: даём потоку шанс ошибочно проснуться.
     */
    static void assertStillBlocked(Thread t) throws InterruptedException {
        Thread.sleep(50);
        if (!t.isAlive()) {
            fail("Поток " + t.getName() + " должен был остаться заблокированным, но завершился");
        }
    }
}
