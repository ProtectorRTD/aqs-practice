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
    @DisplayName("A new latch is closed")
    void initiallyClosed() throws Exception {
        assertFalse(latch.isSignalled());
        assertEquals(0, latch.getQueueLength());
        assertFalse(latch.await(20, TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("After signal() the latch is open forever")
    void signalOpens() throws Exception {
        latch.signal();
        assertTrue(latch.isSignalled());
        latch.await(); // must not block
        assertTrue(latch.await(0, TimeUnit.MILLISECONDS));
        assertTrue(latch.isSignalled());
    }

    @Test
    @DisplayName("A repeated signal() is safe")
    void signalIsIdempotent() throws Exception {
        latch.signal();
        latch.signal();
        assertTrue(latch.isSignalled());
        latch.await();
    }

    @Test
    @DisplayName("A waiting thread wakes up after signal()")
    void waiterWakesUp() throws Exception {
        TestThreads.Worker w = start("waiter", latch::await);
        awaitQueueLength(latch::getQueueLength, 1);
        awaitParked(w);
        latch.signal();
        w.joinOk();
    }

    @Test
    @DisplayName("One signal() wakes ALL waiters (shared mode)")
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
    @DisplayName("Timed await gets a signal() from another thread")
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
    @DisplayName("await is interruptible, and the latch stays closed afterwards")
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
        assertTrue(interrupted.get(), "expected InterruptedException");
        assertFalse(latch.isSignalled());

        // the latch keeps working
        TestThreads.Worker w2 = start("waiter-after", latch::await);
        awaitQueueLength(latch::getQueueLength, 1);
        latch.signal();
        w2.joinOk();
    }
}
