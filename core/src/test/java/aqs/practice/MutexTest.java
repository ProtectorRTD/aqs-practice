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
    @DisplayName("Новый лок свободен и очередь пуста")
    void freshLockIsFree() {
        assertFalse(mutex.isLocked());
        assertFalse(mutex.isHeldByCurrentThread());
        assertEquals(0, mutex.getQueueLength());
    }

    @Test
    @DisplayName("lock/unlock меняют состояние")
    void lockAndUnlock() {
        mutex.lock();
        assertTrue(mutex.isLocked());
        assertTrue(mutex.isHeldByCurrentThread());
        mutex.unlock();
        assertFalse(mutex.isLocked());
        assertFalse(mutex.isHeldByCurrentThread());
    }

    @Test
    @DisplayName("tryLock на свободном локе успешен")
    void tryLockOnFreeLock() {
        assertTrue(mutex.tryLock());
        assertTrue(mutex.isHeldByCurrentThread());
        mutex.unlock();
    }

    @Test
    @DisplayName("Лок невозвратный: повторный tryLock владельцем даёт false")
    void notReentrant() {
        mutex.lock();
        assertFalse(mutex.tryLock());
        assertTrue(mutex.isHeldByCurrentThread());
        mutex.unlock();
        assertFalse(mutex.isLocked());
    }

    @Test
    @DisplayName("unlock без захвата бросает IllegalMonitorStateException")
    void unlockWithoutHolding() {
        assertThrows(IllegalMonitorStateException.class, mutex::unlock);
        assertFalse(mutex.isLocked());
    }

    @Test
    @DisplayName("unlock чужого лока бросает IllegalMonitorStateException и не отпускает лок")
    void unlockByNonOwner() throws Exception {
        try (LockHolder holder = LockHolder.hold(mutex)) {
            assertFalse(mutex.tryLock());
            assertThrows(IllegalMonitorStateException.class, mutex::unlock);
            assertTrue(mutex.isLocked(), "чужой unlock не должен был отпустить лок");
            assertFalse(mutex.tryLock());
        }
        assertFalse(mutex.isLocked());
    }

    @Test
    @DisplayName("Заблокированный lock() просыпается после unlock()")
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
    @DisplayName("Несколько ждущих по очереди получают лок")
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
    @DisplayName("lockInterruptibly отвечает на прерывание и не ломает лок")
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
        assertTrue(interrupted.get(), "ожидался InterruptedException");
        assertTrue(mutex.isHeldByCurrentThread(), "лок должен остаться у владельца");
        mutex.unlock();
        assertTrue(mutex.tryLock(), "после отменённого ожидания лок должен нормально захватываться");
        mutex.unlock();
    }

    @Test
    @DisplayName("tryLock с таймаутом возвращает false, если лок не отпустили")
    void tryLockTimesOut() throws Exception {
        try (LockHolder ignored = LockHolder.hold(mutex)) {
            assertFalse(mutex.tryLock(50, TimeUnit.MILLISECONDS));
        }
        assertTrue(mutex.tryLock(), "после таймаута лок должен нормально захватываться");
        mutex.unlock();
    }

    @Test
    @DisplayName("tryLock с таймаутом успевает, если лок отпустили вовремя")
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
    @DisplayName("Счётчик под локом не теряет инкременты")
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
    @DisplayName("Condition: await отпускает лок, signal будит")
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
        assertTrue(mutex.tryLock(), "await должен был отпустить лок");
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
    @DisplayName("Condition без лока бросает IllegalMonitorStateException")
    void conditionWithoutLock() {
        Condition c = mutex.newCondition();
        assertThrows(IllegalMonitorStateException.class, c::signal);
        assertThrows(IllegalMonitorStateException.class, c::await);
    }
}
