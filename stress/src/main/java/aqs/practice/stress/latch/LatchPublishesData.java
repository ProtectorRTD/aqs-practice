package aqs.practice.stress.latch;

import aqs.practice.OneShotLatch;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.concurrent.TimeUnit;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/**
 * Запись до signal() обязана быть видна после await(): release/acquire в AQS дают happens-before
 * через volatile state. Обычное (не volatile) поле здесь специально.
 */
@JCStressTest
@Outcome(id = "42", expect = ACCEPTABLE, desc = "Данные опубликованы")
@Outcome(id = "-1", expect = FORBIDDEN, desc = "await не дождался signal(): сигнал потерян")
@Outcome(expect = FORBIDDEN, desc = "После await() видно старое значение")
@State
public class LatchPublishesData {

    private final OneShotLatch latch = new OneShotLatch();
    private int data;

    @Actor
    public void publisher() {
        data = 42;
        latch.signal();
    }

    @Actor
    public void consumer(I_Result r) {
        // await с таймаутом: если сигнал изредка теряется, тест покажет -1, а не повесит весь прогон.
        // (В непрерывном режиме jcstress акторы не могут бросать checked-исключения.)
        try {
            r.r1 = latch.await(5, TimeUnit.SECONDS) ? data : -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            r.r1 = -2;
        }
    }
}
