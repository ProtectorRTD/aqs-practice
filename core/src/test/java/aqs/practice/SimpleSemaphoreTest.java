package aqs.practice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static aqs.practice.TestThreads.assertStillBlocked;
import static aqs.practice.TestThreads.awaitParked;
import static aqs.practice.TestThreads.awaitQueueLength;
import static aqs.practice.TestThreads.start;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(30)
class SimpleSemaphoreTest {

    @Test
    @DisplayName("Отрицательное число разрешений в конструкторе запрещено")
    void negativePermitsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SimpleSemaphore(-1));
        assertThrows(IllegalArgumentException.class, () -> new SimpleSemaphore(-1, true));
    }

    @Test
    @DisplayName("Отрицательные аргументы acquire/tryAcquire/release запрещены")
    void negativeArgumentsRejected() {
        SimpleSemaphore s = new SimpleSemaphore(1);
        assertThrows(IllegalArgumentException.class, () -> s.acquire(-1));
        assertThrows(IllegalArgumentException.class, () -> s.tryAcquire(-1));
        assertThrows(IllegalArgumentException.class, () -> s.release(-1));
        assertEquals(1, s.availablePermits());
    }

    @Test
    @DisplayName("acquire/release меняют число разрешений")
    void acquireRelease() throws Exception {
        SimpleSemaphore s = new SimpleSemaphore(3);
        assertEquals(3, s.availablePermits());
        s.acquire();
        s.acquire(2);
        assertEquals(0, s.availablePermits());
        s.release(2);
        assertEquals(2, s.availablePermits());
        s.release();
        assertEquals(3, s.availablePermits());
    }

    @Test
    @DisplayName("tryAcquire: true, если хватает, иначе false без изменения состояния")
    void tryAcquire() {
        SimpleSemaphore s = new SimpleSemaphore(2);
        assertFalse(s.tryAcquire(3));
        assertEquals(2, s.availablePermits());
        assertTrue(s.tryAcquire(2));
        assertFalse(s.tryAcquire());
        assertEquals(0, s.availablePermits());
    }

    @Test
    @DisplayName("Отпустить может любой поток, разрешений может стать больше начального")
    void releaseWithoutAcquire() {
        SimpleSemaphore s = new SimpleSemaphore(0);
        s.release(5);
        assertEquals(5, s.availablePermits());
    }

    @Test
    @DisplayName("acquire блокируется и просыпается после release")
    void acquireBlocksUntilRelease() throws Exception {
        SimpleSemaphore s = new SimpleSemaphore(0);
        TestThreads.Worker w = start("acquirer", s::acquire);
        awaitQueueLength(s::getQueueLength, 1);
        awaitParked(w);
        s.release();
        w.joinOk();
        assertEquals(0, s.availablePermits());
    }

    @Test
    @DisplayName("release(2) будит двух ждущих (propagation в shared-режиме)")
    void releaseWakesSeveral() throws Exception {
        SimpleSemaphore s = new SimpleSemaphore(0);
        TestThreads.Worker a = start("a", s::acquire);
        TestThreads.Worker b = start("b", s::acquire);
        awaitQueueLength(s::getQueueLength, 2);
        awaitParked(a);
        awaitParked(b);
        s.release(2);
        a.joinOk();
        b.joinOk();
        assertEquals(0, s.availablePermits());
    }

    @Test
    @DisplayName("Ждущий acquire(3) не проходит, пока не накопится 3 разрешения")
    void waitsForEnoughPermits() throws Exception {
        SimpleSemaphore s = new SimpleSemaphore(0);
        TestThreads.Worker w = start("needs-3", () -> s.acquire(3));
        awaitQueueLength(s::getQueueLength, 1);
        s.release();
        s.release();
        assertStillBlocked(w);
        assertEquals(2, s.availablePermits());
        s.release();
        w.joinOk();
        assertEquals(0, s.availablePermits());
    }

    @Test
    @DisplayName("acquire прерываемый, разрешения не теряются")
    void acquireIsInterruptible() throws Exception {
        SimpleSemaphore s = new SimpleSemaphore(0);
        AtomicBoolean interrupted = new AtomicBoolean();
        TestThreads.Worker w = start("interruptible", () -> {
            try {
                s.acquire();
            } catch (InterruptedException e) {
                interrupted.set(true);
            }
        });
        awaitQueueLength(s::getQueueLength, 1);
        awaitParked(w);
        w.interrupt();
        w.joinOk();
        assertTrue(interrupted.get(), "ожидался InterruptedException");
        s.release();
        assertEquals(1, s.availablePermits());
    }

    @Test
    @DisplayName("tryAcquire с таймаутом: false, если разрешений нет")
    void timedTryAcquireTimesOut() throws Exception {
        SimpleSemaphore s = new SimpleSemaphore(0);
        assertFalse(s.tryAcquire(30, TimeUnit.MILLISECONDS));
        assertEquals(0, s.availablePermits());
    }

    @Test
    @DisplayName("isFair отражает режим")
    void fairnessFlag() {
        assertFalse(new SimpleSemaphore(1).isFair());
        assertFalse(new SimpleSemaphore(1, false).isFair());
        assertTrue(new SimpleSemaphore(1, true).isFair());
    }

    @Test
    @DisplayName("Честный режим: tryAcquire не обгоняет очередь")
    void fairDoesNotBarge() throws Exception {
        SimpleSemaphore s = new SimpleSemaphore(0, true);
        TestThreads.Worker w = start("needs-2", () -> s.acquire(2));
        awaitQueueLength(s::getQueueLength, 1);
        awaitParked(w);
        s.release();               // 1 разрешение свободно, но в очереди ждут 2
        assertFalse(s.tryAcquire(), "в честном режиме нельзя обгонять очередь");
        assertEquals(1, s.availablePermits());
        s.release();
        w.joinOk();
        assertEquals(0, s.availablePermits());
    }

    @Test
    @DisplayName("Нечестный режим: tryAcquire обгоняет очередь (barging)")
    void nonfairBarges() throws Exception {
        SimpleSemaphore s = new SimpleSemaphore(0, false);
        TestThreads.Worker w = start("needs-2", () -> s.acquire(2));
        awaitQueueLength(s::getQueueLength, 1);
        awaitParked(w);
        s.release();
        assertTrue(s.tryAcquire(), "в нечестном режиме свободное разрешение можно взять без очереди");
        assertEquals(0, s.availablePermits());
        s.release(2);
        w.joinOk();
    }

    @Test
    @DisplayName("Одновременно внутри не больше permits потоков")
    void boundsConcurrency() throws Exception {
        for (boolean fair : new boolean[]{false, true}) {
            int permits = 3;
            SimpleSemaphore s = new SimpleSemaphore(permits, fair);
            AtomicInteger inside = new AtomicInteger();
            AtomicInteger maxInside = new AtomicInteger();
            TestThreads.Worker[] ws = new TestThreads.Worker[8];
            for (int i = 0; i < ws.length; i++) {
                ws[i] = start("worker-" + i, () -> {
                    for (int j = 0; j < 2_000; j++) {
                        s.acquireUninterruptibly();
                        try {
                            int now = inside.incrementAndGet();
                            maxInside.accumulateAndGet(now, Math::max);
                            Thread.onSpinWait();
                            inside.decrementAndGet();
                        } finally {
                            s.release();
                        }
                    }
                });
            }
            for (TestThreads.Worker w : ws) {
                w.joinOk();
            }
            assertTrue(maxInside.get() <= permits,
                    "fair=" + fair + ": внутри одновременно было " + maxInside.get() + " потоков");
            assertEquals(permits, s.availablePermits(), "fair=" + fair + ": разрешения потерялись или размножились");
        }
    }
}
