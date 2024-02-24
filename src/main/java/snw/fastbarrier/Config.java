package snw.fastbarrier;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

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

    private static @Nullable Material materialConf(String path) {
        String raw = conf().getString(path);
        if (raw == null) {
            return null;
        }
        return Material.matchMaterial(raw);
    }

    private static <T> T matchOrElse(T val, Predicate<T> predicate, @NonNull T def) {
        if (val != null && predicate.test(val)) {
            return val;
        } else {
            Preconditions.checkArgument(predicate.test(def),
                    "else value must also match the predicate");
            return def;
        }
    }

    public static Material newBlockType() {
        return matchOrElse(materialConf("block"), Material::isBlock, Material.BARRIER);
    }
}
