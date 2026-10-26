package aqs.practice.stress.rwlock;

import aqs.practice.SimpleReadWriteLock;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.ZZ_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/**
 * Два читателя одновременно делают tryLock на свободном локе.
 * Оба обязаны пройти: CAS одного из них может проиграть, и это не повод отказывать.
 */
@JCStressTest
@Outcome(id = "true, true", expect = ACCEPTABLE, desc = "Оба читателя внутри")
@Outcome(expect = FORBIDDEN, desc = "Читателю отказали без писателя: ложная неудача на CAS")
@State
public class RwReadersShareTryLock {

    private final SimpleReadWriteLock rw = new SimpleReadWriteLock();

    @Actor
    public void reader1(ZZ_Result r) {
        r.r1 = rw.readLock().tryLock();
    }

    @Actor
    public void reader2(ZZ_Result r) {
        r.r2 = rw.readLock().tryLock();
    }
}
