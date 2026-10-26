package aqs.practice.stress.reentrant;

import aqs.practice.SimpleReentrantLock;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/** Режим: честный. Инкремент под двойным (возвратным) захватом лока. */
@JCStressTest
@Outcome(id = "2", expect = ACCEPTABLE, desc = "Оба инкремента на месте")
@Outcome(expect = FORBIDDEN, desc = "Инкремент потерян")
@State
public class FairReentrantMutualExclusion {

    private final SimpleReentrantLock lock = new SimpleReentrantLock(true);
    private int x;

    private void incrementTwiceLocked() {
        lock.lock();
        try {
            lock.lock();
            try {
                x++;
            } finally {
                lock.unlock();
            }
        } finally {
            lock.unlock();
        }
    }

    @Actor
    public void actor1() {
        incrementTwiceLocked();
    }

    @Actor
    public void actor2() {
        incrementTwiceLocked();
    }

    @Arbiter
    public void arbiter(I_Result r) {
        r.r1 = x;
    }
}
