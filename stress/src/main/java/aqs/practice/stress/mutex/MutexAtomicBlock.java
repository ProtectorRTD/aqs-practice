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
 * The writer writes x, then y under the lock. The reader reads y, then x under the same lock.
 * If the lock works (and gives happens-before), the reader sees either "before" or "after", never the middle.
 */
@JCStressTest
@Outcome(id = "0, 0", expect = ACCEPTABLE, desc = "The reader got in before the writer")
@Outcome(id = "1, 1", expect = ACCEPTABLE, desc = "The reader got in after the writer")
@Outcome(expect = FORBIDDEN, desc = "The reader saw a half-written state")
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
