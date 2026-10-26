package aqs.practice.stress.semaphore;

import aqs.practice.SimpleSemaphore;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/** Семафор с одним разрешением как мьютекс: инкременты не теряются, разрешение возвращается. */
@JCStressTest
@Outcome(id = "2, 1", expect = ACCEPTABLE, desc = "Оба инкремента на месте, разрешение вернулось")
@Outcome(expect = FORBIDDEN, desc = "Потерян инкремент или разрешение")
@State
public class SemaphoreAsMutex {

    private final SimpleSemaphore semaphore = new SimpleSemaphore(1);
    private int x;

    @Actor
    public void actor1() {
        semaphore.acquireUninterruptibly();
        try {
            x++;
        } finally {
            semaphore.release();
        }
    }

    @Actor
    public void actor2() {
        semaphore.acquireUninterruptibly();
        try {
            x++;
        } finally {
            semaphore.release();
        }
    }

    @Arbiter
    public void arbiter(II_Result r) {
        r.r1 = x;
        r.r2 = semaphore.availablePermits();
    }
}
