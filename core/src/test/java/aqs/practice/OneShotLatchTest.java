package aqs.practice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static aqs.practice.TestThreads.awaitParked;
import static aqs.practice.TestThreads.awaitQueueLength;
import static aqs.practice.TestThreads.start;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(30)
class OneShotLatchTest {

    private final OneShotLatch latch = new OneShotLatch();

    @Test
    @DisplayName("Новая защёлка закрыта")
    void initiallyClosed() throws Exception {
        assertFalse(latch.isSignalled());
        assertEquals(0, latch.getQueueLength());
        assertFalse(latch.await(20, TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("После signal() защёлка открыта навсегда")
    void signalOpens() throws Exception {
        latch.signal();
        assertTrue(latch.isSignalled());
        latch.await(); // не должен блокироваться
        assertTrue(latch.await(0, TimeUnit.MILLISECONDS));
        assertTrue(latch.isSignalled());
    }

    @Test
    @DisplayName("Повторный signal() безопасен")
    void signalIsIdempotent() throws Exception {
        latch.signal();
        latch.signal();
        assertTrue(latch.isSignalled());
        latch.await();
    }

    @Test
    @DisplayName("Ждущий поток просыпается после signal()")
    void waiterWakesUp() throws Exception {
        TestThreads.Worker w = start("waiter", latch::await);
        awaitQueueLength(latch::getQueueLength, 1);
        awaitParked(w);
        latch.signal();
        w.joinOk();
    }

    @Test
    @DisplayName("Один signal() будит ВСЕХ ждущих (shared-режим)")
    void signalWakesAllWaiters() throws Exception {
        int n = 6;
        TestThreads.Worker[] ws = new TestThreads.Worker[n];
        for (int i = 0; i < n; i++) {
            ws[i] = start("waiter-" + i, latch::await);
        }
        awaitQueueLength(latch::getQueueLength, n);
        for (TestThreads.Worker w : ws) {
            awaitParked(w);
        }
        latch.signal();
        for (TestThreads.Worker w : ws) {
            w.joinOk();
        }
        assertEquals(0, latch.getQueueLength());
    }

    @Test
    @DisplayName("await с таймаутом дожидается signal() из другого потока")
    void timedAwaitSucceeds() throws Exception {
        AtomicBoolean result = new AtomicBoolean();
        TestThreads.Worker w = start("timed", () ->
                result.set(latch.await(TestThreads.TIMEOUT_MS, TimeUnit.MILLISECONDS)));
        awaitQueueLength(latch::getQueueLength, 1);
        latch.signal();
        w.joinOk();
        assertTrue(result.get());
    }

    @Test
    @DisplayName("await прерываемый, защёлка после этого по-прежнему закрыта")
    void awaitIsInterruptible() throws Exception {
        AtomicBoolean interrupted = new AtomicBoolean();
        TestThreads.Worker w = start("interruptible", () -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                interrupted.set(true);
            }
        });
        awaitQueueLength(latch::getQueueLength, 1);
        awaitParked(w);
        w.interrupt();
        w.joinOk();
        assertTrue(interrupted.get(), "ожидался InterruptedException");
        assertFalse(latch.isSignalled());

        // защёлка продолжает работать
        TestThreads.Worker w2 = start("waiter-after", latch::await);
        awaitQueueLength(latch::getQueueLength, 1);
        latch.signal();
        w2.joinOk();
    }
}
