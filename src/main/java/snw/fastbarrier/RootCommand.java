package snw.fastbarrier;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static snw.fastbarrier.PlayerUtils.playSound;

@Command(name = "barrier")
@Permission("op")
public final class RootCommand {
    private final Map<UUID, BukkitTask> inQueueForceDestroy = new HashMap<>();

    @Execute(name = "height")
    void height(@Context Player sender, @Arg int newValue) {
        Session.getOrCreate(sender).setHeight(newValue);
        ok(sender, "操作成功。(注意: 之前的可撤销操作不受新的墙高度影响! 设置的值只对你自己有效)");
    }

    @Execute(name = "new")
    @Permission("op")
    void newSession(@Context Player sender) {
        if (Session.has(sender)) {
            ok(sender, "你已经有会话啦，去围墙吧~ 扔出两次屏障即可确认放置！");
        } else {
            Session.getOrCreate(sender);
            sender.getInventory().addItem(new ItemStack(Material.BARRIER), new ItemStack(Material.STICK));
            ok(sender, "OK! 开始吧! 扔出两次屏障确认放置，扔出木棍(继续/暂停)记录！");
        }
    }

    @Execute(name = "done")
    void destroySession(@Context Player sender) {
        if (Session.has(sender)) {
            final Session session = Session.getOrCreate(sender);
            final UUID uuid = sender.getUniqueId();
            if (session.getQueuedLocation().isEmpty() || inQueueForceDestroy.containsKey(uuid)) {
                final BukkitTask task = inQueueForceDestroy.remove(uuid); // may not destroy by force?
                if (task != null) {
                    task.cancel();
                }
                session.destroy();
                ok(sender, "已销毁会话。");
            } else {
                sender.sendMessage(ChatColor.RED +
                        "检测到你的会话中还有未提交的区域！如果要强制销毁会话，请在 10 秒内再次执行此命令。");
                playSound(sender, Sound.ENTITY_VILLAGER_TRADE);
                final BukkitTask task = FastBarrier.instance.getServer()
                        .getScheduler()
                        .runTaskLater(FastBarrier.instance,
                                () -> inQueueForceDestroy.remove(uuid), 200L);
                inQueueForceDestroy.put(uuid, task);
            }
        } else {
            fail(sender, "没有待销毁的会话。");
        }
    }

    @Execute(name = "undo")
    void undo(@Context Player sender) {
        if (Session.has(sender)) {
            boolean success = Session.getOrCreate(sender).undo();
            if (success) {
                ok(sender, "已撤销上一次操作。");
            } else {
                fail(sender, "没有可撤销的操作！");
            }
        } else {
            fail(sender, "你还没有会话！");
        }
    }

    @Execute(name = "redo")
    void redo(@Context Player sender) {
        if (Session.has(sender)) {
            boolean success = Session.getOrCreate(sender).redo();
            if (success) {
                ok(sender, "已重新执行上一个被撤销的操作。");
            } else {
                fail(sender, "没有可重新执行的操作！");
            }
        } else {
            fail(sender, "你还没有会话！");
        }
    }

    @Execute(name = "block")
    void block(@Context Player sender, @Arg Material type) {
        if (Session.has(sender)) {
            Session.getOrCreate(sender).setBlockType(type);
            ok(sender, "操作成功。(这个设置只对你自己生效)");
        } else {
            fail(sender, "你还没有会话！");
        }
    }

    @Execute(name = "reload")
    void reload(@Context CommandSender sender) {
        FastBarrier.instance.reloadConfig();
        if (sender instanceof Player player) {
            ok(player, "操作成功。");
        } else {
            sender.sendMessage("操作成功。");
        }
    }

    private void ok(Player player, String message) {
        player.sendMessage(ChatColor.GREEN + message);
        playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    private void fail(Player player, String message) {
        player.sendMessage(ChatColor.RED + message);
        playSound(player, Sound.ENTITY_VILLAGER_NO);
    }
}
