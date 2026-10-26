package aqs.practice.stress.semaphore;

import aqs.practice.SimpleSemaphore;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Mode;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.Signal;
import org.openjdk.jcstress.annotations.State;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/**
 * Два ждущих по одному разрешению, один release(2). Оба должны пройти:
 * проверяет, что tryAcquireShared сообщает AQS «осталось ещё» и пробуждение идёт дальше по очереди.
 * Второго ждущего актор запускает сам (Termination разрешает одного актора).
 */
@JCStressTest(Mode.Termination)
@Outcome(id = "TERMINATED", expect = ACCEPTABLE, desc = "Оба ждущих прошли")
@Outcome(id = "STALE", expect = FORBIDDEN, desc = "Один из ждущих завис, хотя разрешений хватало")
@State
public class SemaphoreReleasePropagation {

    private final SimpleSemaphore semaphore = new SimpleSemaphore(0);

    @Actor
    public void acquirers() throws InterruptedException {
        Thread second = new Thread(semaphore::acquireUninterruptibly);
        second.start();
        semaphore.acquireUninterruptibly();
        second.join();
    }

    @Signal
    public void signal() {
        semaphore.release(2);
    }
}
