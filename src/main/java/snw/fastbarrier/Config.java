package snw.fastbarrier;

public final class Config {
    private Config() {
    }

    private static int ifLargerThanZeroOrElse(int expectedToBeLargerThanZero, int defaultValue) {
        return expectedToBeLargerThanZero > 0 ? expectedToBeLargerThanZero : defaultValue;
    }

    private static int intConf(String path) {
        return FastBarrier.instance.getConfig().getInt(path);
    }

    public static int processedUnitPerTick() {
        return ifLargerThanZeroOrElse(intConf("unit-per-tick"), 1);
    }

    public static int wallHeight() {
        return ifLargerThanZeroOrElse(intConf("wall-height"), 50);
    }

    public static int queueWarnThreshold() {
        return ifLargerThanZeroOrElse(intConf("queue-warn-threshold"), 300);
    }

    public static int maxUndoSteps() {
        return ifLargerThanZeroOrElse(intConf("max-undo-steps"), Integer.MAX_VALUE);
    }
}
