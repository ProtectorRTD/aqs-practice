package aqs.practice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;

import static aqs.practice.TestThreads.awaitParked;
import static aqs.practice.TestThreads.awaitQueueLength;
import static aqs.practice.TestThreads.start;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(30)
class MutexTest {

    private final Mutex mutex = new Mutex();

    @Test
    @DisplayName("A new lock is free and the queue is empty")
    void freshLockIsFree() {
        assertFalse(mutex.isLocked());
        assertFalse(mutex.isHeldByCurrentThread());
        assertEquals(0, mutex.getQueueLength());
    }

    @Test
    @DisplayName("lock/unlock change the state")
    void lockAndUnlock() {
        mutex.lock();
        assertTrue(mutex.isLocked());
        assertTrue(mutex.isHeldByCurrentThread());
        mutex.unlock();
        assertFalse(mutex.isLocked());
        assertFalse(mutex.isHeldByCurrentThread());
    }

    @Test
    @DisplayName("tryLock on a free lock succeeds")
    void tryLockOnFreeLock() {
        assertTrue(mutex.tryLock());
        assertTrue(mutex.isHeldByCurrentThread());
        mutex.unlock();
    }

    @Test
    @DisplayName("The lock is non-reentrant: a repeated tryLock by the owner returns false")
    void notReentrant() {
        mutex.lock();
        assertFalse(mutex.tryLock());
        assertTrue(mutex.isHeldByCurrentThread());
        mutex.unlock();
        assertFalse(mutex.isLocked());
    }

    @Test
    @DisplayName("unlock without holding throws IllegalMonitorStateException")
    void unlockWithoutHolding() {
        assertThrows(IllegalMonitorStateException.class, mutex::unlock);
        assertFalse(mutex.isLocked());
    }

    @Test
    @DisplayName("unlock of someone else's lock throws IllegalMonitorStateException and does not release it")
    void unlockByNonOwner() throws Exception {
        try (LockHolder holder = LockHolder.hold(mutex)) {
            assertFalse(mutex.tryLock());
            assertThrows(IllegalMonitorStateException.class, mutex::unlock);
            assertTrue(mutex.isLocked(), "a non-owner unlock must not release the lock");
            assertFalse(mutex.tryLock());
        }
        assertFalse(mutex.isLocked());
    }

    @Test
    @DisplayName("A blocked lock() wakes up after unlock()")
    void blockedLockWakesUp() throws Exception {
        mutex.lock();
        TestThreads.Worker w = start("waiter", () -> {
            mutex.lock();
            mutex.unlock();
        });
        awaitQueueLength(mutex::getQueueLength, 1);
        awaitParked(w);
        mutex.unlock();
        w.joinOk();
        assertFalse(mutex.isLocked());
    }

    @Test
    @DisplayName("Several waiters get the lock one after another")
    void severalWaitersAllProceed() throws Exception {
        mutex.lock();
        TestThreads.Worker[] ws = new TestThreads.Worker[3];
        for (int i = 0; i < ws.length; i++) {
            ws[i] = start("waiter-" + i, () -> {
                mutex.lock();
                mutex.unlock();
            });
        }
        awaitQueueLength(mutex::getQueueLength, ws.length);
        mutex.unlock();
        for (TestThreads.Worker w : ws) {
            w.joinOk();
        }
        assertFalse(mutex.isLocked());
        assertEquals(0, mutex.getQueueLength());
    }

    @Test
    @DisplayName("lockInterruptibly responds to interruption and does not break the lock")
    void lockInterruptibly() throws Exception {
        mutex.lock();
        AtomicBoolean interrupted = new AtomicBoolean();
        TestThreads.Worker w = start("interruptible", () -> {
            try {
                mutex.lockInterruptibly();
                mutex.unlock();
            } catch (InterruptedException e) {
                interrupted.set(true);
            }
        });
        awaitQueueLength(mutex::getQueueLength, 1);
        awaitParked(w);
        w.interrupt();
        w.joinOk();
        assertTrue(interrupted.get(), "expected InterruptedException");
        assertTrue(mutex.isHeldByCurrentThread(), "the lock must stay with its owner");
        mutex.unlock();
        assertTrue(mutex.tryLock(), "after a cancelled wait the lock must be acquirable as usual");
        mutex.unlock();
    }

    @Test
    @DisplayName("Timed tryLock returns false if the lock is not released")
    void tryLockTimesOut() throws Exception {
        try (LockHolder ignored = LockHolder.hold(mutex)) {
            assertFalse(mutex.tryLock(50, TimeUnit.MILLISECONDS));
        }
        assertTrue(mutex.tryLock(), "after a timeout the lock must be acquirable as usual");
        mutex.unlock();
    }

    @Test
    @DisplayName("Timed tryLock succeeds if the lock is released in time")
    void tryLockWithTimeoutSucceeds() throws Exception {
        mutex.lock();
        AtomicBoolean got = new AtomicBoolean();
        TestThreads.Worker w = start("timed", () -> {
            got.set(mutex.tryLock(TestThreads.TIMEOUT_MS, TimeUnit.MILLISECONDS));
            if (got.get()) {
                mutex.unlock();
            }
        });
        awaitQueueLength(mutex::getQueueLength, 1);
        mutex.unlock();
        w.joinOk();
        assertTrue(got.get());
    }

    @Test
    @DisplayName("A counter under the lock loses no increments")
    void mutualExclusionCounter() throws Exception {
        int threads = 4;
        int iterations = 20_000;
        int[] counter = {0};
        TestThreads.Worker[] ws = new TestThreads.Worker[threads];
        for (int i = 0; i < threads; i++) {
            ws[i] = start("inc-" + i, () -> {
                for (int j = 0; j < iterations; j++) {
                    mutex.lock();
                    try {
                        counter[0]++;
                    } finally {
                        mutex.unlock();
                    }
                }
            });
        }
        for (TestThreads.Worker w : ws) {
            w.joinOk();
        }
        assertEquals(threads * iterations, counter[0]);
    }

    @Test
    @DisplayName("Condition: await releases the lock, signal wakes up")
    void conditionAwaitSignal() throws Exception {
        Condition ready = mutex.newCondition();
        AtomicBoolean flag = new AtomicBoolean();
        TestThreads.Worker w = start("awaiter", () -> {
            mutex.lock();
            try {
                while (!flag.get()) {
                    ready.await();
                }
            } finally {
                mutex.unlock();
            }
        });
        awaitParked(w);
        assertTrue(mutex.tryLock(), "await should have released the lock");
        try {
            flag.set(true);
            ready.signal();
        } finally {
            mutex.unlock();
        }
        w.joinOk();
        assertFalse(mutex.isLocked());
    }

    @Test
    @DisplayName("Condition without the lock throws IllegalMonitorStateException")
    void conditionWithoutLock() {
        Condition c = mutex.newCondition();
        assertThrows(IllegalMonitorStateException.class, c::signal);
        assertThrows(IllegalMonitorStateException.class, c::await);
    }
}
