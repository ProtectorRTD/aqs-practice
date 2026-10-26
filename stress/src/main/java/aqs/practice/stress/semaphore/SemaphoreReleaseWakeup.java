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

/** acquire() на пустом семафоре должен проснуться после release() из другого потока. */
@JCStressTest(Mode.Termination)
@Outcome(id = "TERMINATED", expect = ACCEPTABLE, desc = "Ждущий проснулся")
@Outcome(id = "STALE", expect = FORBIDDEN, desc = "Сигнал потерян, поток завис")
@State
public class SemaphoreReleaseWakeup {

    private final SimpleSemaphore semaphore = new SimpleSemaphore(0);

    @Actor
    public void acquirer() throws InterruptedException {
        semaphore.acquire();
    }

    @Signal
    public void signal() {
        semaphore.release();
    }
}
