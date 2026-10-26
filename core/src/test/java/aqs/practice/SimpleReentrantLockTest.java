package aqs.practice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

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

@Timeout(30)
class SimpleReentrantLockTest {

    @Test
    @DisplayName("isFair отражает режим")
    void fairnessFlag() {
        assertFalse(new SimpleReentrantLock().isFair());
        assertFalse(new SimpleReentrantLock(false).isFair());
        assertTrue(new SimpleReentrantLock(true).isFair());
    }

    @Test
    @DisplayName("Повторный захват увеличивает hold count, освобождение — только после стольких же unlock")
    void reentrancy() {
        for (boolean fair : new boolean[]{false, true}) {
            SimpleReentrantLock lock = new SimpleReentrantLock(fair);
            assertEquals(0, lock.getHoldCount());
            lock.lock();
            lock.lock();
            assertTrue(lock.tryLock(), "fair=" + fair + ": владелец должен проходить повторно");
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
    }

    @Test
    @DisplayName("unlock без захвата бросает IllegalMonitorStateException")
    void unlockWithoutHolding() {
        SimpleReentrantLock lock = new SimpleReentrantLock();
        assertThrows(IllegalMonitorStateException.class, lock::unlock);
    }

    @Test
    @DisplayName("unlock чужого лока бросает IllegalMonitorStateException и не отпускает лок")
    void unlockByNonOwner() throws Exception {
        SimpleReentrantLock lock = new SimpleReentrantLock();
        try (LockHolder ignored = LockHolder.hold(lock)) {
            assertThrows(IllegalMonitorStateException.class, lock::unlock);
            assertTrue(lock.isLocked());
            assertFalse(lock.isHeldByCurrentThread());
            assertEquals(0, lock.getHoldCount());
            assertFalse(lock.tryLock());
        }
        assertFalse(lock.isLocked());
    }

    @Test
    @DisplayName("Ждущий проходит только после полного освобождения")
    void waiterProceedsAfterFullRelease() throws Exception {
        SimpleReentrantLock lock = new SimpleReentrantLock();
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

    @Test
    @DisplayName("lockInterruptibly отвечает на прерывание")
    void lockInterruptibly() throws Exception {
        SimpleReentrantLock lock = new SimpleReentrantLock();
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

    @Test
    @DisplayName("tryLock с таймаутом возвращает false, если лок занят")
    void tryLockTimesOut() throws Exception {
        SimpleReentrantLock lock = new SimpleReentrantLock();
        try (LockHolder ignored = LockHolder.hold(lock)) {
            assertFalse(lock.tryLock(30, TimeUnit.MILLISECONDS));
        }
        assertTrue(lock.tryLock(30, TimeUnit.MILLISECONDS));
        lock.unlock();
    }

    @Test
    @DisplayName("await полностью отпускает лок и восстанавливает hold count")
    void awaitReleasesFullyAndRestoresHoldCount() throws Exception {
        SimpleReentrantLock lock = new SimpleReentrantLock();
        Condition cond = lock.newCondition();
        AtomicBoolean flag = new AtomicBoolean();
        AtomicInteger holdAfterAwait = new AtomicInteger(-1);
        TestThreads.Worker w = start("awaiter", () -> {
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
            }
        });
        awaitParked(w);
        assertTrue(lock.tryLock(), "await должен полностью отпустить лок, даже захваченный дважды");
        try {
            flag.set(true);
            cond.signal();
        } finally {
            lock.unlock();
        }
        w.joinOk();
        assertEquals(2, holdAfterAwait.get(), "после await hold count должен восстановиться");
        assertFalse(lock.isLocked());
    }

    @Test
    @DisplayName("signal будит одного, signalAll — всех")
    void signalVsSignalAll() throws Exception {
        SimpleReentrantLock lock = new SimpleReentrantLock();
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
        awaitCondition(() -> finished.get() == 1, "signal должен был разбудить одного");
        Thread.sleep(50);
        assertEquals(1, finished.get(), "signal разбудил больше одного ждущего");

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

    @Test
    @DisplayName("Condition без лока бросает IllegalMonitorStateException")
    void conditionWithoutLock() {
        SimpleReentrantLock lock = new SimpleReentrantLock();
        Condition c = lock.newCondition();
        assertThrows(IllegalMonitorStateException.class, c::signal);
        assertThrows(IllegalMonitorStateException.class, c::signalAll);
        assertThrows(IllegalMonitorStateException.class, c::await);
    }

    @Test
    @DisplayName("Честный режим: сразу после unlock чужой tryLock не обгоняет ждущего")
    void fairDoesNotBarge() throws Exception {
        SimpleReentrantLock lock = new SimpleReentrantLock(true);
        for (int round = 0; round < 20; round++) {
            CountDownLatch checked = new CountDownLatch(1);
            lock.lock();
            TestThreads.Worker w = start("queued-" + round, () -> {
                lock.lock();
                try {
                    // держим лок, пока основной поток не сделал проверку
                    checked.await(TestThreads.TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } finally {
                    lock.unlock();
                }
            });
            awaitQueueLength(lock::getQueueLength, 1);
            awaitParked(w);
            lock.unlock();
            // Лок либо ещё свободен, но в очереди ждёт w, либо его уже держит w.
            // В обоих случаях честный tryLock обязан вернуть false.
            boolean barged = lock.tryLock();
            if (barged) {
                lock.unlock();
            }
            checked.countDown();
            assertFalse(barged, "раунд " + round + ": честный лок не должен пускать без очереди");
            w.joinOk();
        }
    }

    @Test
    @DisplayName("Счётчик под локом (с повторным захватом) не теряет инкременты")
    void mutualExclusionCounter() throws Exception {
        for (boolean fair : new boolean[]{false, true}) {
            SimpleReentrantLock lock = new SimpleReentrantLock(fair);
            int threads = 4;
            int iterations = fair ? 3_000 : 20_000; // честный режим заметно медленнее под конкуренцией
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
            assertEquals(threads * iterations, counter[0], "fair=" + fair);
            assertFalse(lock.isLocked());
        }
    }
}
