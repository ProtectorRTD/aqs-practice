package aqs.practice.stress.mutex;

import aqs.practice.Mutex;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/**
 * Два потока делают {@code x++} под локом. Потерянный инкремент значит,
 * что лок пустил внутрь двоих сразу.
 */
@JCStressTest
@Outcome(id = "2", expect = ACCEPTABLE, desc = "Оба инкремента на месте")
@Outcome(expect = FORBIDDEN, desc = "Инкремент потерян: два потока были в критической секции одновременно")
@State
public class MutexMutualExclusion {

    private final Mutex lock = new Mutex();
    private int x;

    @Actor
    public void actor1() {
        lock.lock();
        try {
            x++;
        } finally {
            lock.unlock();
        }
    }

    @Actor
    public void actor2() {
        lock.lock();
        try {
            x++;
        } finally {
            lock.unlock();
        }
    }

    @Arbiter
    public void arbiter(I_Result r) {
        r.r1 = x;
    }
}
