package sh.miles.powerai.lua;

public final class TickUtils {

    public static final float MINECRAFT_MS_PER_TICK = (float) ((1.0 / 20.0) * 1000.0);

    public static long tickBudgetMillis(float proportion) {
        return (long) (MINECRAFT_MS_PER_TICK * proportion);
    }

    public static final long MINECRAFT_3MS_TICK = tickBudgetMillis(0.06f);
    public static final long MINECRAFT_5MS_TICK = tickBudgetMillis(0.10f);
    public static final long MINECRAFT_10MS_TICK = tickBudgetMillis(0.20f);

    private TickUtils() {
    }
}
