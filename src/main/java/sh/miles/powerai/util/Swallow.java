package sh.miles.powerai.util;

public interface Swallow<E extends Throwable> {
    void run() throws E;

    static <E extends Throwable> void swallow(Swallow<E> swallow) {
        try {
            swallow.run();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
