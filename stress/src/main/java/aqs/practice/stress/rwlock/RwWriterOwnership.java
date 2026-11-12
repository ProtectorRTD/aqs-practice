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
 * Two writers each take the write lock, check that they own it, and unlock.
 * Both must stay the owner until their own unlock, and both unlocks must succeed.
 *
 * <p>Catches the wrong order in {@code tryRelease}: if the previous writer clears the owner
 * <i>after</i> publishing {@code state == 0}, the next writer may already have set itself
 * as the owner, and that write gets wiped. The next writer then fails its ownership check
 * or gets {@link IllegalMonitorStateException} from its own unlock.
 */
@JCStressTest
@Outcome(id = "true, true", expect = ACCEPTABLE, desc = "Both writers owned the lock and unlocked it")
@Outcome(expect = FORBIDDEN, desc = "A writer lost ownership while holding the lock")
@State
public class RwWriterOwnership {

    private final SimpleReadWriteLock rw = new SimpleReadWriteLock();

    private boolean lockCheckUnlock() {
        rw.writeLock().lock();
        boolean owned = rw.isWriteLockedByCurrentThread();
        try {
            rw.writeLock().unlock();
            return owned;
        } catch (IllegalMonitorStateException e) {
            return false;
        }
    }

    @Actor
    public void writer1(ZZ_Result r) {
        r.r1 = lockCheckUnlock();
    }

    @Actor
    public void writer2(ZZ_Result r) {
        r.r2 = lockCheckUnlock();
    }
}
