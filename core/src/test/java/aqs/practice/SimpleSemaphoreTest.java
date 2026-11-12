package aqs.practice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

/**
 * Tests that involve blocking and waking up run in both modes ({@code fair=false/true}):
 * fair mode must keep the same shared-mode semantics, it only adds "do not overtake the queue".
 */
@Timeout(30)
class SimpleSemaphoreTest {

    @Test
    @DisplayName("A negative number of permits in the constructor is rejected")
    void negativePermitsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SimpleSemaphore(-1));
        assertThrows(IllegalArgumentException.class, () -> new SimpleSemaphore(-1, true));
    }

    @Test
    @DisplayName("Negative arguments to acquire/tryAcquire/release are rejected")
    void negativeArgumentsRejected() {
        SimpleSemaphore s = new SimpleSemaphore(1);
        assertThrows(IllegalArgumentException.class, () -> s.acquire(-1));
        assertThrows(IllegalArgumentException.class, () -> s.tryAcquire(-1));
        assertThrows(IllegalArgumentException.class, () -> s.release(-1));
        assertEquals(1, s.availablePermits());
    }

    @ParameterizedTest(name = "fair={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("acquire/release change the number of permits")
    void acquireRelease(boolean fair) throws Exception {
        SimpleSemaphore s = new SimpleSemaphore(3, fair);
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
    @DisplayName("tryAcquire: true if there are enough permits, otherwise false with no state change")
    void tryAcquire() {
        SimpleSemaphore s = new SimpleSemaphore(2);
        assertFalse(s.tryAcquire(3));
        assertEquals(2, s.availablePermits());
        assertTrue(s.tryAcquire(2));
        assertFalse(s.tryAcquire());
        assertEquals(0, s.availablePermits());
    }

    @Test
    @DisplayName("Any thread may release; permits may exceed the initial count")
    void releaseWithoutAcquire() {
        SimpleSemaphore s = new SimpleSemaphore(0);
        s.release(5);
        assertEquals(5, s.availablePermits());
    }

    @ParameterizedTest(name = "fair={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("acquire blocks and wakes up after release")
    void acquireBlocksUntilRelease(boolean fair) throws Exception {
        SimpleSemaphore s = new SimpleSemaphore(0, fair);
        TestThreads.Worker w = start("acquirer", s::acquire);
        awaitQueueLength(s::getQueueLength, 1);
        awaitParked(w);
        s.release();
        w.joinOk();
        assertEquals(0, s.availablePermits());
    }

    @ParameterizedTest(name = "fair={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("release(2) wakes two waiters (propagation in shared mode)")
    void releaseWakesSeveral(boolean fair) throws Exception {
        SimpleSemaphore s = new SimpleSemaphore(0, fair);
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

    @ParameterizedTest(name = "fair={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("release(3) wakes three waiters, none of which ever calls release")
    void releaseWakesWholeChain(boolean fair) throws Exception {
        SimpleSemaphore s = new SimpleSemaphore(0, fair);
        TestThreads.Worker[] ws = new TestThreads.Worker[3];
        for (int i = 0; i < ws.length; i++) {
            ws[i] = start("waiter-" + i, s::acquire);
        }
        awaitQueueLength(s::getQueueLength, ws.length);
        for (TestThreads.Worker w : ws) {
            awaitParked(w);
        }
        s.release(ws.length);
        for (TestThreads.Worker w : ws) {
            w.joinOk();
        }
        assertEquals(0, s.availablePermits());
        assertEquals(0, s.getQueueLength());
    }

    @ParameterizedTest(name = "fair={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("A waiting acquire(3) does not proceed until 3 permits accumulate")
    void waitsForEnoughPermits(boolean fair) throws Exception {
        SimpleSemaphore s = new SimpleSemaphore(0, fair);
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

    @ParameterizedTest(name = "fair={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("acquire is interruptible and no permits are lost")
    void acquireIsInterruptible(boolean fair) throws Exception {
        SimpleSemaphore s = new SimpleSemaphore(0, fair);
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
        assertTrue(interrupted.get(), "expected InterruptedException");
        s.release();
        assertEquals(1, s.availablePermits());
    }

    @ParameterizedTest(name = "fair={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("Timed tryAcquire: false if there are no permits")
    void timedTryAcquireTimesOut(boolean fair) throws Exception {
        SimpleSemaphore s = new SimpleSemaphore(0, fair);
        assertFalse(s.tryAcquire(30, TimeUnit.MILLISECONDS));
        assertEquals(0, s.availablePermits());
    }

    @Test
    @DisplayName("isFair reflects the mode")
    void fairnessFlag() {
        assertFalse(new SimpleSemaphore(1).isFair());
        assertFalse(new SimpleSemaphore(1, false).isFair());
        assertTrue(new SimpleSemaphore(1, true).isFair());
    }

    @Test
    @DisplayName("Fair mode: tryAcquire does not overtake the queue")
    void fairDoesNotBarge() throws Exception {
        SimpleSemaphore s = new SimpleSemaphore(0, true);
        TestThreads.Worker w = start("needs-2", () -> s.acquire(2));
        awaitQueueLength(s::getQueueLength, 1);
        awaitParked(w);
        s.release();               // 1 permit is free, but the queued thread needs 2
        assertFalse(s.tryAcquire(), "fair mode must not overtake the queue");
        assertEquals(1, s.availablePermits());
        s.release();
        w.joinOk();
        assertEquals(0, s.availablePermits());
    }

    @Test
    @DisplayName("Nonfair mode: tryAcquire overtakes the queue (barging)")
    void nonfairBarges() throws Exception {
        SimpleSemaphore s = new SimpleSemaphore(0, false);
        TestThreads.Worker w = start("needs-2", () -> s.acquire(2));
        awaitQueueLength(s::getQueueLength, 1);
        awaitParked(w);
        s.release();
        assertTrue(s.tryAcquire(), "nonfair mode may take a free permit without queueing");
        assertEquals(0, s.availablePermits());
        s.release(2);
        w.joinOk();
    }

    @ParameterizedTest(name = "fair={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("No more than permits threads are inside at once")
    void boundsConcurrency(boolean fair) throws Exception {
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
                maxInside.get() + " threads were inside at the same time");
        assertEquals(permits, s.availablePermits(), "permits were lost or duplicated");
    }
}
