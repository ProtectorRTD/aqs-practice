package aqs.practice.stress.latch;

import aqs.practice.OneShotLatch;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Mode;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.Signal;
import org.openjdk.jcstress.annotations.State;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/**
 * Поток ждёт защёлку, другой её открывает. Классическая проверка на потерянный сигнал:
 * signal() может прийти в любой момент — до постановки в очередь, между постановкой и park, после park.
 */
@JCStressTest(Mode.Termination)
@Outcome(id = "TERMINATED", expect = ACCEPTABLE, desc = "Ждущий проснулся")
@Outcome(id = "STALE", expect = FORBIDDEN, desc = "Сигнал потерян, поток завис")
@State
public class LatchAwaitWakeup {

    private final OneShotLatch latch = new OneShotLatch();

    @Actor
    public void awaiter() throws InterruptedException {
        latch.await();
    }

    @Signal
    public void signal() {
        latch.signal();
    }
}
