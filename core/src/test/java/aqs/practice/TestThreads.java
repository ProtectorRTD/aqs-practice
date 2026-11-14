package aqs.practice;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Utilities for deterministic multithreaded JUnit tests.
 * No "lucky" sleeps: we wait for an observable event (thread queued, thread finished)
 * with a hard timeout.
 */
final class TestThreads {

    static final long TIMEOUT_MS = 5_000;

    private TestThreads() {
    }

    @FunctionalInterface
    interface Body {
        void run() throws Throwable;
    }

    /** A thread that remembers the exception thrown by its body so the test can check it. */
    static final class Worker extends Thread {
        private final Body body;
        private final AtomicReference<Throwable> error = new AtomicReference<>();

        Worker(String name, Body body) {
            super(name);
            this.body = body;
            setDaemon(true); // a hung thread does not keep the JVM alive after a failed test
        }

        @Override
        public void run() {
            try {
                body.run();
            } catch (Throwable t) {
                error.set(t);
            }
        }

        /** Waits for completion; fails if the thread did not finish or threw an exception. */
        void joinOk() throws InterruptedException {
            join(TIMEOUT_MS);
            if (isAlive()) {
                fail("Thread " + getName() + " did not finish in " + TIMEOUT_MS + " ms (lost wakeup?)");
            }
            Throwable t = error.get();
            if (t != null) {
                throw new AssertionError("Thread " + getName() + " threw an exception", t);
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

    /** Waits until at least {@code expected} threads are in the synchronizer's queue. */
    static void awaitQueueLength(IntSupplier queueLength, int expected) throws InterruptedException {
        awaitCondition(() -> queueLength.getAsInt() >= expected,
                expected + " threads did not show up in the queue");
    }

    static void awaitCondition(BooleanSupplier condition, String message) throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT_MS * 1_000_000L;
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("Gave up waiting after " + TIMEOUT_MS + " ms: " + message);
            }
            Thread.sleep(1);
        }
    }

    /** Waits until the thread actually parks, not merely joins the queue. */
    static void awaitParked(Thread t) throws InterruptedException {
        awaitCondition(() -> {
            Thread.State s = t.getState();
            return s == Thread.State.WAITING || s == Thread.State.TIMED_WAITING || s == Thread.State.TERMINATED;
        }, "thread " + t.getName() + " did not park");
        if (!t.isAlive()) {
            fail("Thread " + t.getName() + " should have blocked, but it finished");
        }
    }

    /**
     * Checks that the thread has still not moved on. This is a "negative" check,
     * so it necessarily involves a pause: we give the thread a chance to wake up by mistake.
     */
    static void assertStillBlocked(Thread t) throws InterruptedException {
        Thread.sleep(50);
        if (!t.isAlive()) {
            fail("Thread " + t.getName() + " should have stayed blocked, but it finished");
        }
    }
}
