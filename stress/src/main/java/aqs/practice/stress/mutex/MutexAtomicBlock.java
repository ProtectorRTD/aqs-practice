package aqs.practice.stress.mutex;

import aqs.practice.Mutex;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/**
 * Писатель под локом пишет x, потом y. Читатель под тем же локом читает y, потом x.
 * Если лок работает (и даёт happens-before), читатель видит либо «до», либо «после», но не середину.
 */
@JCStressTest
@Outcome(id = "0, 0", expect = ACCEPTABLE, desc = "Читатель зашёл раньше писателя")
@Outcome(id = "1, 1", expect = ACCEPTABLE, desc = "Читатель зашёл после писателя")
@Outcome(expect = FORBIDDEN, desc = "Читатель увидел полузаписанное состояние")
@State
public class MutexAtomicBlock {

    private final Mutex lock = new Mutex();
    private int x;
    private int y;

    @Actor
    public void writer() {
        lock.lock();
        try {
            x = 1;
            y = 1;
        } finally {
            lock.unlock();
        }
    }

    @Actor
    public void reader(II_Result r) {
        lock.lock();
        try {
            r.r1 = y;
            r.r2 = x;
        } finally {
            lock.unlock();
        }
    }
}
