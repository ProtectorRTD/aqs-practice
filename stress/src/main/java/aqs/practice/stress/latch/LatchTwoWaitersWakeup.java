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
 * Два ждущих, один signal(). Termination-режим jcstress разрешает только одного актора,
 * поэтому второго ждущего актор запускает сам. Ловит сломанную передачу пробуждения
 * по цепочке (propagation): проснётся один, второй зависнет.
 */
@JCStressTest(Mode.Termination)
@Outcome(id = "TERMINATED", expect = ACCEPTABLE, desc = "Оба ждущих проснулись")
@Outcome(id = "STALE", expect = FORBIDDEN, desc = "Один из ждущих не проснулся")
@State
public class LatchTwoWaitersWakeup {

    private final OneShotLatch latch = new OneShotLatch();

    @Actor
    public void awaiters() throws InterruptedException {
        Thread second = new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        second.start();
        latch.await();
        second.join();
    }

    @Signal
    public void signal() {
        latch.signal();
    }
}
