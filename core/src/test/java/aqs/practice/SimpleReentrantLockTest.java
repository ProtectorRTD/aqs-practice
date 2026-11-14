package aqs.practice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;

import static aqs.practice.TestThreads.assertStillBlocked;
import static aqs.practice.TestThreads.awaitCondition;
import static aqs.practice.TestThreads.awaitParked;
import static aqs.practice.TestThreads.awaitQueueLength;
import static aqs.practice.TestThreads.start;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Everything except barging must behave the same in both modes, so most tests run
 * with {@code fair=false} and {@code fair=true}.
 */
@Timeout(30)
class SimpleReentrantLockTest {

    @Test
    @DisplayName("isFair reflects the mode")
    void fairnessFlag() {
        assertFalse(new SimpleReentrantLock().isFair());
        assertFalse(new SimpleReentrantLock(false).isFair());
        assertTrue(new SimpleReentrantLock(true).isFair());
    }

    @ParameterizedTest(name = "fair={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("Reacquiring increases the hold count; the lock is released only after as many unlocks")
    void reentrancy(boolean fair) {
        SimpleReentrantLock lock = new SimpleReentrantLock(fair);
        assertEquals(0, lock.getHoldCount());
        lock.lock();
        lock.lock();
        assertTrue(lock.tryLock(), "the owner must be able to reacquire");
        assertEquals(3, lock.getHoldCount());
        lock.unlock();
        lock.unlock();
        assertTrue(lock.isLocked());
        assertEquals(1, lock.getHoldCount());
        lock.unlock();
        assertFalse(lock.isLocked());
        assertEquals(0, lock.getHoldCount());
        assertFalse(lock.isHeldByCurrentThread());
    }

    @ParameterizedTest(name = "fair={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("unlock without holding throws IllegalMonitorStateException")
    void unlockWithoutHolding(boolean fair) {
        SimpleReentrantLock lock = new SimpleReentrantLock(fair);
        assertThrows(IllegalMonitorStateException.class, lock::unlock);
    }

    @ParameterizedTest(name = "fair={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("unlock of someone else's lock throws IllegalMonitorStateException and does not release it")
    void unlockByNonOwner(boolean fair) throws Exception {
        SimpleReentrantLock lock = new SimpleReentrantLock(fair);
        try (LockHolder ignored = LockHolder.hold(lock)) {
            assertThrows(IllegalMonitorStateException.class, lock::unlock);
            assertTrue(lock.isLocked());
            assertFalse(lock.isHeldByCurrentThread());
            assertEquals(0, lock.getHoldCount());
            assertFalse(lock.tryLock());
        }
        assertFalse(lock.isLocked());
    }

    @ParameterizedTest(name = "fair={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("A waiter proceeds only after a full release")
    void waiterProceedsAfterFullRelease(boolean fair) throws Exception {
        SimpleReentrantLock lock = new SimpleReentrantLock(fair);
        lock.lock();
        lock.lock();
        TestThreads.Worker w = start("waiter", () -> {
            lock.lock();
            lock.unlock();
        });
        awaitQueueLength(lock::getQueueLength, 1);
        lock.unlock();
        assertStillBlocked(w);
        lock.unlock();
        w.joinOk();
        assertFalse(lock.isLocked());
    }

    @ParameterizedTest(name = "fair={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("lockInterruptibly responds to interruption")
    void lockInterruptibly(boolean fair) throws Exception {
        SimpleReentrantLock lock = new SimpleReentrantLock(fair);
        lock.lock();
        AtomicBoolean interrupted = new AtomicBoolean();
        TestThreads.Worker w = start("interruptible", () -> {
            try {
                lock.lockInterruptibly();
                lock.unlock();
            } catch (InterruptedException e) {
                interrupted.set(true);
            }
        });
        awaitQueueLength(lock::getQueueLength, 1);
        awaitParked(w);
        w.interrupt();
        w.joinOk();
        assertTrue(interrupted.get());
        assertEquals(1, lock.getHoldCount());
        lock.unlock();
    }

    @ParameterizedTest(name = "fair={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("Timed tryLock returns false if the lock is held")
    void tryLockTimesOut(boolean fair) throws Exception {
        SimpleReentrantLock lock = new SimpleReentrantLock(fair);
        try (LockHolder ignored = LockHolder.hold(lock)) {
            assertFalse(lock.tryLock(30, TimeUnit.MILLISECONDS));
        }
        assertTrue(lock.tryLock(30, TimeUnit.MILLISECONDS));
        lock.unlock();
    }

    @ParameterizedTest(name = "fair={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("await releases the lock fully and restores the hold count")
    void awaitReleasesFullyAndRestoresHoldCount(boolean fair) throws Exception {
        SimpleReentrantLock lock = new SimpleReentrantLock(fair);
        Condition cond = lock.newCondition();
        AtomicBoolean flag = new AtomicBoolean();
        AtomicInteger holdAfterAwait = new AtomicInteger(-1);
        TestThreads.Worker w = start("awaiter", () -> {
            lock.lock();
            lock.lock();
            lock.lock();
            try {
                while (!flag.get()) {
                    cond.await();
                }
                holdAfterAwait.set(lock.getHoldCount());
            } finally {
                lock.unlock();
                lock.unlock();
                lock.unlock();
            }
        });
        awaitParked(w);
        assertTrue(lock.tryLock(), "await must release the lock fully, even when it is held three times");
        try {
            flag.set(true);
            cond.signal();
        } finally {
            lock.unlock();
        }
        w.joinOk();
        assertEquals(3, holdAfterAwait.get(), "the hold count must be restored after await");
        assertFalse(lock.isLocked());
    }

    @ParameterizedTest(name = "fair={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("signal wakes one, signalAll wakes all")
    void signalVsSignalAll(boolean fair) throws Exception {
        SimpleReentrantLock lock = new SimpleReentrantLock(fair);
        Condition cond = lock.newCondition();
        AtomicInteger permits = new AtomicInteger();
        AtomicInteger finished = new AtomicInteger();
        TestThreads.Worker[] ws = new TestThreads.Worker[3];
        for (int i = 0; i < ws.length; i++) {
            ws[i] = start("awaiter-" + i, () -> {
                lock.lock();
                try {
                    while (permits.get() == 0) {
                        cond.await();
                    }
                    permits.decrementAndGet();
                } finally {
                    lock.unlock();
                }
                finished.incrementAndGet();
            });
        }
        for (TestThreads.Worker w : ws) {
            awaitParked(w);
        }

        lock.lock();
        try {
            permits.incrementAndGet();
            cond.signal();
        } finally {
            lock.unlock();
        }
        awaitCondition(() -> finished.get() == 1, "signal should have woken one waiter");
        Thread.sleep(50);
        assertEquals(1, finished.get(), "signal woke more than one waiter");

        lock.lock();
        try {
            permits.addAndGet(2);
            cond.signalAll();
        } finally {
            lock.unlock();
        }
        for (TestThreads.Worker w : ws) {
            w.joinOk();
        }
        assertEquals(3, finished.get());
    }

    @ParameterizedTest(name = "fair={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("Condition without the lock throws IllegalMonitorStateException")
    void conditionWithoutLock(boolean fair) {
        SimpleReentrantLock lock = new SimpleReentrantLock(fair);
        Condition c = lock.newCondition();
        assertThrows(IllegalMonitorStateException.class, c::signal);
        assertThrows(IllegalMonitorStateException.class, c::signalAll);
        assertThrows(IllegalMonitorStateException.class, c::await);
    }

    @Test
    @DisplayName("Fair mode: right after unlock, another thread's tryLock does not overtake the waiter")
    void fairDoesNotBarge() throws Exception {
        SimpleReentrantLock lock = new SimpleReentrantLock(true);
        for (int round = 0; round < 20; round++) {
            CountDownLatch checked = new CountDownLatch(1);
            lock.lock();
            TestThreads.Worker w = start("queued-" + round, () -> {
                lock.lock();
                try {
                    // hold the lock until the main thread has done its check
                    checked.await(TestThreads.TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } finally {
                    lock.unlock();
                }
            });
            awaitQueueLength(lock::getQueueLength, 1);
            awaitParked(w);
            lock.unlock();
            // The lock is either still free with w waiting in the queue, or already held by w.
            // In both cases a fair tryLock must return false.
            boolean barged = lock.tryLock();
            if (barged) {
                lock.unlock();
            }
            checked.countDown();
            assertFalse(barged, "round " + round + ": a fair lock must not let anyone skip the queue");
            w.joinOk();
        }
    }

    @ParameterizedTest(name = "fair={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("A counter under the lock (with reacquiring) loses no increments")
    void mutualExclusionCounter(boolean fair) throws Exception {
        SimpleReentrantLock lock = new SimpleReentrantLock(fair);
        int threads = 4;
        int iterations = fair ? 3_000 : 20_000; // fair mode is noticeably slower under contention
        int[] counter = {0};
        TestThreads.Worker[] ws = new TestThreads.Worker[threads];
        for (int i = 0; i < threads; i++) {
            ws[i] = start("inc-" + i, () -> {
                for (int j = 0; j < iterations; j++) {
                    lock.lock();
                    try {
                        lock.lock();
                        try {
                            counter[0]++;
                        } finally {
                            lock.unlock();
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            });
        }
        for (TestThreads.Worker w : ws) {
            w.joinOk();
        }
        assertEquals(threads * iterations, counter[0]);
        assertFalse(lock.isLocked());
    }
}
