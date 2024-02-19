package snw.fastbarrier;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public final class PlayerUtils {
    private PlayerUtils() {
    }

    public static void playSound(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, SoundCategory.MASTER, 1, 1);
    }
}
