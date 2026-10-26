package aqs.practice;

/**
 * Заглушка для ещё не написанных методов.
 * Отдельный тип, чтобы тест никогда случайно не «прошёл» на заглушке:
 * ни один тест не ожидает именно это исключение.
 */
public final class NotImplementedYet extends RuntimeException {

    public NotImplementedYet() {
        super("TODO: не реализовано");
    }
}
