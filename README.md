# AQS practice

Exercises for Doug Lea's paper *The java.util.concurrent Synchronizer Framework* (2004):
implement synchronizers on top of the JDK's `AbstractQueuedSynchronizer` and check them with
JUnit (semantics) and [jcstress](https://github.com/openjdk/jcstress) (concurrency).

`main` contains only the skeleton: public API, contracts in javadoc, and tests. Implementations live in personal branches.

## Levels

| Level | Class | Focus |
|---|---|---|
| 1 | `Mutex` | exclusive mode, owner, `isHeldExclusively`, `ConditionObject` |
| 2 | `OneShotLatch` | shared mode where acquire waits for a condition instead of taking anything |
| 3 | `SimpleSemaphore` | shared mode with a count, CAS loop, `tryAcquireShared` return value, fair/nonfair subclasses |
| 4 | `SimpleReentrantLock` | recursion in `state`, the `acquires` argument, `Condition` with nested holds |
| 5 | `SimpleReadWriteLock` | one `int state` for two modes, writer starvation |

Each class's contract is in its javadoc; tests check exactly that. Deviations from the JDK
(e.g. fair `tryAcquire()` does not barge) are stated there.

Rules: use only AQS inside your classes (no JDK locks/semaphores), don't edit the tests.

## Requirements

- JDK 17+
- Maven is not needed: use the wrapper (`./mvnw`, or `mvnw.cmd` on Windows)
- jcstress: 4+ cores recommended; tests with three actors are skipped on 2 cores

## JUnit

```bash
./mvnw -pl core test                                   # all
./mvnw -pl core test -Dtest=MutexTest                  # one level
./mvnw -pl core test -Dtest='MutexTest#blockedLockWakesUp'
```

Unimplemented classes fail with `NotImplementedYet`. Multithreaded tests wait for observable events
(thread queued, thread parked) with a 5 s timeout; "did not finish in 5000 ms" usually means a lost wakeup.

## jcstress

```bash
./mvnw -pl stress -am package -DskipTests
java -jar stress/target/jcstress.jar -t aqs.practice.stress.mutex -m quick
java -jar stress/target/jcstress.jar -t aqs.practice
```

`-t` is a regex on the test name; `-m` is one of `sanity`, `quick`, `default`, `tough`, `stress`.
Summary is printed at the end (`Failed tests: ...`), HTML report goes to `results/index.html`.

| Package | Tests |
|---|---|
| `mutex` | mutual exclusion, atomic block (happens-before), `tryLock` arbitration, `Condition` wakeup |
| `latch` | wakeup, two waiters woken by one signal, data publication via `signal`/`await` |
| `semaphore` | `tryAcquire` with three threads (nonfair and fair), semaphore as mutex, wakeup, `release(2)` waking two |
| `reentrant` | mutual exclusion with nested locking (nonfair and fair), `tryLock`, `await` with nested holds |
| `rwlock` | exclusive writers, reader sees whole write, shared readers, writer vs reader |

`Mode.Termination` tests check that a thread wakes up at all; `STALE` means it hung.

Run jcstress for a level only after its JUnit tests pass: if wakeup is always broken, continuous jcstress tests just hang.

CI runs JUnit and jcstress `-m sanity` per level on every branch except `main`.
