package aqs.practice.stress.rwlock;

import aqs.practice.SimpleReadWriteLock;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.ZZ_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/** Писатель и читатель одновременно делают tryLock на свободном локе: ровно один проходит. */
@JCStressTest
@Outcome(id = {"true, false", "false, true"}, expect = ACCEPTABLE, desc = "Прошёл ровно один")
@Outcome(id = "true, true", expect = FORBIDDEN, desc = "Писатель и читатель внутри одновременно")
@Outcome(id = "false, false", expect = FORBIDDEN, desc = "Ложная неудача: лок был свободен")
@State
public class RwWriterVsReaderTryLock {

    private final SimpleReadWriteLock rw = new SimpleReadWriteLock();

    @Actor
    public void writer(ZZ_Result r) {
        r.r1 = rw.writeLock().tryLock();
    }

    @Actor
    public void reader(ZZ_Result r) {
        r.r2 = rw.readLock().tryLock();
    }
}
