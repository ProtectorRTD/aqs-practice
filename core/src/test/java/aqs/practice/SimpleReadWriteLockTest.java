package aqs.practice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import static aqs.practice.TestThreads.assertStillBlocked;
import static aqs.practice.TestThreads.awaitParked;
import static aqs.practice.TestThreads.awaitQueueLength;
import static aqs.practice.TestThreads.start;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(30)
class SimpleReadWriteLockTest {

    private final SimpleReadWriteLock rw = new SimpleReadWriteLock();
    private final Lock read = rw.readLock();
    private final Lock write = rw.writeLock();

    @Test
    @DisplayName("Новый лок свободен")
    void freshLockIsFree() {
        assertEquals(0, rw.getReadLockCount());
        assertFalse(rw.isWriteLocked());
        assertFalse(rw.isWriteLockedByCurrentThread());
        assertEquals(0, rw.getQueueLength());
    }

    @Test
    @DisplayName("newCondition не поддерживается")
    void noConditions() {
        assertThrows(UnsupportedOperationException.class, read::newCondition);
        assertThrows(UnsupportedOperationException.class, write::newCondition);
    }

    @Test
    @DisplayName("Несколько читателей держат лок одновременно")
    void readersShare() throws Exception {
        read.lock();
        try (LockHolder ignored = LockHolder.hold(read)) {
            assertEquals(2, rw.getReadLockCount());
            assertFalse(rw.isWriteLocked());
        }
        assertEquals(1, rw.getReadLockCount());
        read.unlock();
        assertEquals(0, rw.getReadLockCount());
    }

    @Test
    @DisplayName("Писатель исключает и читателей, и других писателей")
    void writerIsExclusive() throws Exception {
        try (LockHolder ignored = LockHolder.hold(write)) {
            assertTrue(rw.isWriteLocked());
            assertFalse(rw.isWriteLockedByCurrentThread());
            assertFalse(read.tryLock());
            assertFalse(write.tryLock());
        }
        assertFalse(rw.isWriteLocked());
        assertTrue(write.tryLock());
        assertTrue(rw.isWriteLockedByCurrentThread());
        write.unlock();
    }

    @Test
    @DisplayName("Читатели не пускают писателя, повышения read→write нет")
    void readersBlockWriter() throws Exception {
        read.lock();
        assertFalse(write.tryLock(), "upgrade read→write не поддерживается");
        AtomicBoolean otherGot = new AtomicBoolean(true);
        start("other-writer", () -> otherGot.set(write.tryLock())).joinOk();
        assertFalse(otherGot.get());
        read.unlock();
        assertTrue(write.tryLock());
        write.unlock();
    }

    @Test
    @DisplayName("Оба лока невозвратные, понижения write→read нет")
    void notReentrant() {
        write.lock();
        assertFalse(write.tryLock());
        assertFalse(read.tryLock(), "downgrade write→read не поддерживается");
        write.unlock();
        assertFalse(rw.isWriteLocked());
        assertEquals(0, rw.getReadLockCount());
    }

    @Test
    @DisplayName("Неверный unlock бросает IllegalMonitorStateException")
    void illegalUnlocks() throws Exception {
        assertThrows(IllegalMonitorStateException.class, read::unlock);
        assertThrows(IllegalMonitorStateException.class, write::unlock);
        try (LockHolder ignored = LockHolder.hold(write)) {
            assertThrows(IllegalMonitorStateException.class, write::unlock);
            assertTrue(rw.isWriteLocked(), "чужой unlock не должен был отпустить write-лок");
        }
        assertEquals(0, rw.getReadLockCount());
        assertFalse(rw.isWriteLocked());
    }

    @Test
    @DisplayName("Ждущий писатель не даёт новым читателям пройти, а после ухода читателей получает лок")
    void waitingWriterBlocksNewReaders() throws Exception {
        read.lock();
        TestThreads.Worker writer = start("writer", () -> {
            write.lock();
            write.unlock();
        });
        awaitQueueLength(rw::getQueueLength, 1);
        awaitParked(writer);

        AtomicBoolean newReaderGot = new AtomicBoolean(true);
        start("late-reader", () -> {
            newReaderGot.set(read.tryLock());
            if (newReaderGot.get()) {
                read.unlock();
            }
        }).joinOk();
        assertFalse(newReaderGot.get(), "пока ждёт писатель, новый читатель не должен проходить");

        read.unlock();
        writer.joinOk();
        assertFalse(rw.isWriteLocked());
    }

    @Test
    @DisplayName("Освобождение write-лока будит всех ждущих читателей сразу")
    void writerReleaseWakesAllReaders() throws Exception {
        int readers = 4;
        CountDownLatch allInside = new CountDownLatch(readers);
        write.lock();
        TestThreads.Worker[] ws = new TestThreads.Worker[readers];
        for (int i = 0; i < readers; i++) {
            ws[i] = start("reader-" + i, () -> {
                read.lock();
                try {
                    allInside.countDown();
                    // Ждём, пока внутрь зайдут все: это возможно, только если read-лок
                    // действительно разделяемый и пробуждение прошло по цепочке.
                    if (!allInside.await(TestThreads.TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                        throw new AssertionError("читатели не смогли держать лок одновременно");
                    }
                } finally {
                    read.unlock();
                }
            });
        }
        awaitQueueLength(rw::getQueueLength, readers);
        write.unlock();
        for (TestThreads.Worker w : ws) {
            w.joinOk();
        }
        assertEquals(0, rw.getReadLockCount());
    }

    @Test
    @DisplayName("Писатель ждёт, пока уйдёт последний читатель")
    void writerWaitsForLastReader() throws Exception {
        read.lock();
        try (LockHolder second = LockHolder.hold(read)) {
            TestThreads.Worker writer = start("writer", () -> {
                write.lock();
                write.unlock();
            });
            awaitQueueLength(rw::getQueueLength, 1);
            read.unlock();
            assertStillBlocked(writer);
            second.close();
            writer.joinOk();
        }
    }

    @Test
    @DisplayName("Больше 65535 читателей — Error")
    void readCountOverflow() {
        int max = 65_535;
        for (int i = 0; i < max; i++) {
            assertTrue(read.tryLock());
        }
        assertEquals(max, rw.getReadLockCount());
        assertThrows(Error.class, read::tryLock);
        assertEquals(max, rw.getReadLockCount(), "неудачный захват не должен менять счётчик");
        for (int i = 0; i < max; i++) {
            read.unlock();
        }
        assertEquals(0, rw.getReadLockCount());
    }

    @Test
    @DisplayName("Читатели всегда видят согласованное состояние, которое пишут писатели")
    void readersSeeConsistentState() throws Exception {
        int[] pair = {0, 0};
        AtomicBoolean stop = new AtomicBoolean();
        TestThreads.Worker[] writers = new TestThreads.Worker[2];
        TestThreads.Worker[] readers = new TestThreads.Worker[3];
        for (int i = 0; i < writers.length; i++) {
            writers[i] = start("writer-" + i, () -> {
                for (int j = 0; j < 5_000; j++) {
                    write.lock();
                    try {
                        pair[0]++;
                        Thread.onSpinWait();
                        pair[1]++;
                    } finally {
                        write.unlock();
                    }
                }
            });
        }
        for (int i = 0; i < readers.length; i++) {
            readers[i] = start("reader-" + i, () -> {
                while (!stop.get()) {
                    read.lock();
                    try {
                        if (pair[0] != pair[1]) {
                            throw new AssertionError("читатель увидел полузаписанное состояние: " + pair[0] + " / " + pair[1]);
                        }
                    } finally {
                        read.unlock();
                    }
                }
            });
        }
        for (TestThreads.Worker w : writers) {
            w.joinOk();
        }
        stop.set(true);
        for (TestThreads.Worker r : readers) {
            r.joinOk();
        }
        assertEquals(10_000, pair[0]);
        assertEquals(10_000, pair[1]);
    }
}
