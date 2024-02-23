package snw.fastbarrier;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public final class Config {
    private Config() {
    }

    private static FileConfiguration conf() {
        return FastBarrier.instance.getConfig();
    }

    private static int ifLargerThanZeroOrElse(int expectedToBeLargerThanZero, int defaultValue) {
        return expectedToBeLargerThanZero > 0 ? expectedToBeLargerThanZero : defaultValue;
    }

    private static int intConf(String path) {
        return conf().getInt(path);
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

    private static Material materialConf(String path, Material def) {
        String raw = conf().getString(path);
        if (raw == null) {
            return def;
        }
        final Material material = Material.matchMaterial(raw);
        if (material != null && material.isBlock()) {
            return material;
        } else {
            return def;
        }
    }

    public static Material newBlockType() {
        return materialConf("block", Material.BARRIER);
    }
}
