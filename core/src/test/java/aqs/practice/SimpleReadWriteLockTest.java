package aqs.practice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

import static aqs.practice.TestThreads.assertStillBlocked;
import static aqs.practice.TestThreads.awaitCondition;
import static aqs.practice.TestThreads.awaitParked;
import static aqs.practice.TestThreads.awaitQueueLength;
import static aqs.practice.TestThreads.start;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(30)
class SimpleReadWriteLockTest {

    private final SimpleReadWriteLock rw = new SimpleReadWriteLock();
    private final Lock read = rw.readLock();
    private final Lock write = rw.writeLock();

    @Test
    @DisplayName("A new lock is free")
    void freshLockIsFree() {
        assertEquals(0, rw.getReadLockCount());
        assertFalse(rw.isWriteLocked());
        assertFalse(rw.isWriteLockedByCurrentThread());
        assertEquals(0, rw.getQueueLength());
    }

    @Test
    @DisplayName("newCondition is not supported")
    void noConditions() {
        assertThrows(UnsupportedOperationException.class, read::newCondition);
        assertThrows(UnsupportedOperationException.class, write::newCondition);
    }

    @Test
    @DisplayName("Several readers hold the lock at the same time")
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
    @DisplayName("A writer excludes both readers and other writers")
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
    @DisplayName("Readers keep the writer out; there is no read→write upgrade")
    void readersBlockWriter() throws Exception {
        read.lock();
        assertFalse(write.tryLock(), "read→write upgrade is not supported");
        AtomicBoolean otherGot = new AtomicBoolean(true);
        start("other-writer", () -> otherGot.set(write.tryLock())).joinOk();
        assertFalse(otherGot.get());
        read.unlock();
        assertTrue(write.tryLock());
        write.unlock();
    }

    @Test
    @DisplayName("Both locks are non-reentrant; there is no write→read downgrade")
    void notReentrant() {
        write.lock();
        assertFalse(write.tryLock());
        assertFalse(read.tryLock(), "write→read downgrade is not supported");
        write.unlock();
        assertFalse(rw.isWriteLocked());
        assertEquals(0, rw.getReadLockCount());
    }

    @Test
    @DisplayName("A wrong unlock throws IllegalMonitorStateException")
    void illegalUnlocks() throws Exception {
        assertThrows(IllegalMonitorStateException.class, read::unlock);
        assertThrows(IllegalMonitorStateException.class, write::unlock);
        try (LockHolder ignored = LockHolder.hold(write)) {
            assertThrows(IllegalMonitorStateException.class, write::unlock);
            assertTrue(rw.isWriteLocked(), "a non-owner unlock must not release the write lock");
        }
        assertEquals(0, rw.getReadLockCount());
        assertFalse(rw.isWriteLocked());
    }

    @Test
    @DisplayName("readLock().unlock() while only a writer holds the lock throws IllegalMonitorStateException")
    void readUnlockWhileWriteLocked() throws Exception {
        try (LockHolder ignored = LockHolder.hold(write)) {
            assertThrows(IllegalMonitorStateException.class, read::unlock,
                    "nobody holds the read lock, even though the write lock is held by another thread");
            assertTrue(rw.isWriteLocked(), "a wrong read unlock must not touch the write lock");
            assertEquals(0, rw.getReadLockCount());
        }

        write.lock();
        try {
            assertThrows(IllegalMonitorStateException.class, read::unlock,
                    "the writer itself does not hold the read lock either");
            assertTrue(rw.isWriteLockedByCurrentThread());
        } finally {
            write.unlock();
        }
        assertFalse(rw.isWriteLocked());
    }

    @Test
    @DisplayName("Timed readLock().tryLock takes a read hold, not the write lock")
    void timedReadTryLockTakesReadHold() throws Exception {
        assertTrue(read.tryLock(50, TimeUnit.MILLISECONDS));
        try {
            assertEquals(1, rw.getReadLockCount());
            assertFalse(rw.isWriteLocked(), "a timed read tryLock must not take the write lock");

            // the hold is shared: another reader gets in at once
            AtomicBoolean otherGot = new AtomicBoolean();
            start("other-reader", () -> {
                otherGot.set(read.tryLock(50, TimeUnit.MILLISECONDS));
                if (otherGot.get()) {
                    read.unlock();
                }
            }).joinOk();
            assertTrue(otherGot.get(), "a second reader must share the lock");
        } finally {
            read.unlock();
        }
        assertEquals(0, rw.getReadLockCount());
    }

    @Test
    @DisplayName("Timed readLock().tryLock returns false while a writer holds the lock")
    void timedReadTryLockTimesOut() throws Exception {
        try (LockHolder ignored = LockHolder.hold(write)) {
            assertFalse(read.tryLock(30, TimeUnit.MILLISECONDS));
            assertEquals(0, rw.getReadLockCount());
        }
        assertFalse(rw.isWriteLocked());
    }

    @Test
    @DisplayName("Timed readLock().tryLock gets in once the writer leaves")
    void timedReadTryLockWaitsForWriter() throws Exception {
        write.lock();
        AtomicBoolean got = new AtomicBoolean();
        TestThreads.Worker w = start("timed-reader", () -> {
            got.set(read.tryLock(TestThreads.TIMEOUT_MS, TimeUnit.MILLISECONDS));
            if (got.get()) {
                read.unlock();
            }
        });
        awaitQueueLength(rw::getQueueLength, 1);
        write.unlock();
        w.joinOk();
        assertTrue(got.get());
        assertEquals(0, rw.getReadLockCount());
        assertFalse(rw.isWriteLocked());
    }

    @Test
    @DisplayName("Under contention each writer stays the owner until its own unlock")
    void writerOwnershipUnderContention() throws Exception {
        int threads = 4;
        int iterations = 100_000;
        AtomicReference<String> problem = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            start("writer-" + i, () -> {
                for (int j = 0; j < iterations && problem.get() == null; j++) {
                    write.lock();
                    if (!rw.isWriteLockedByCurrentThread()) {
                        problem.compareAndSet(null, "a writer holds the lock but is not its owner");
                    }
                    try {
                        write.unlock();
                    } catch (IllegalMonitorStateException e) {
                        // the previous owner cleared the owner field after this thread had already set it
                        problem.compareAndSet(null, "a writer got IllegalMonitorStateException from its own unlock");
                    }
                }
                done.countDown();
            });
        }
        awaitCondition(() -> done.getCount() == 0 || problem.get() != null, "writers did not finish");
        assertNull(problem.get());
        assertFalse(rw.isWriteLocked());
    }

    @Test
    @DisplayName("A waiting writer keeps new readers out and gets the lock once the readers leave")
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
        assertFalse(newReaderGot.get(), "while a writer waits, a new reader must not get in");

        read.unlock();
        writer.joinOk();
        assertFalse(rw.isWriteLocked());
    }

    @Test
    @DisplayName("Releasing the write lock wakes all waiting readers at once")
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
                    // Wait until everyone is inside: this is only possible if the read lock
                    // is really shared and the wakeup went down the chain.
                    if (!allInside.await(TestThreads.TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                        throw new AssertionError("the readers could not hold the lock at the same time");
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
    @DisplayName("The writer waits until the last reader leaves")
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
    @DisplayName("More than 65535 readers throws Error")
    void readCountOverflow() {
        int max = 65_535;
        for (int i = 0; i < max; i++) {
            assertTrue(read.tryLock());
        }
        assertEquals(max, rw.getReadLockCount());
        assertThrows(Error.class, read::tryLock);
        assertEquals(max, rw.getReadLockCount(), "a failed acquire must not change the count");
        for (int i = 0; i < max; i++) {
            read.unlock();
        }
        assertEquals(0, rw.getReadLockCount());
    }

    @Test
    @DisplayName("Readers always see the consistent state written by writers")
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
                            throw new AssertionError("a reader saw a half-written state: " + pair[0] + " / " + pair[1]);
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
