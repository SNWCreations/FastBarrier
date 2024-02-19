package snw.fastbarrier;

import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.bukkit.LiteCommandsBukkit;
import dev.rollczi.litecommands.schematic.Schematic;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static snw.fastbarrier.PlayerUtils.playSound;

public final class FastBarrier extends JavaPlugin implements Listener {
    // Check if it is null before use.
    public static FastBarrier instance;
    private final Map<UUID, BukkitTask> aboutToQueue = new HashMap<>();
    private LiteCommands<CommandSender> commands;

    @Override
    public void onLoad() {
        instance = this;
        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        //noinspection UnstableApiUsage
        commands = LiteCommandsBukkit.builder("fastbarrier", this)
                .commands(
                        new RootCommand()
                )
                .invalidUsage((invocation, result, chain) -> {
                    final CommandSender sender = invocation.sender();
                    final Schematic schematic = result.getSchematic();
                    if (schematic.isOnlyFirst()) {
                        sender.sendMessage(ChatColor.RED + "命令语法错误！ (" + schematic.first() + ")");
                    } else {
                        sender.sendMessage(ChatColor.RED + "命令语法错误！");
                        for (String s : schematic.all()) {
                            sender.sendMessage(" - " + s);
                        }
                    }
                    if (sender instanceof Player player) {
                        playSound(player, Sound.ENTITY_VILLAGER_NO);
                    }
                })
                .build(true);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, new BlockPlacer(), 1L, 1L);
    }

    @Override
    public void onDisable() {
        instance = null;
        if (commands != null) {
            commands.unregister();
        }
        // Plugin shutdown logic
        aboutToQueue.clear();
        Session.destroyAll();
    }

    private boolean inSuitableOperatorMode(Player player) {
        return player.isOp() && player.getGameMode() == GameMode.CREATIVE;
    }
    
    private boolean inOperate(Player player) {
        return Session.has(player) && Session.getOrCreate(player).isActive();
    }

    private boolean barrierRelated(BlockEvent event) {
        return event.getBlock().getType() == Material.BARRIER;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (barrierRelated(event)) {
            final Block placed = event.getBlockPlaced();
            final Player player = event.getPlayer();
            if (inSuitableOperatorMode(player)) {
                if (inOperate(player)) {
                    final Session session = Session.getOrCreate(player);
                    final Location location = placed.getLocation();
                    final int current = session.append(location);
                    final int threshold = Config.queueWarnThreshold();
                    if (current >= threshold) {
                        player.sendMessage(ChatColor.GOLD + "等等！你的队列中已经有 " + current + " 处方块了！要不要先提交一下？");
                        playSound(player, Sound.BLOCK_ANVIL_PLACE);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDropStick(PlayerDropItemEvent event) {
        final ItemStack stack = event.getItemDrop().getItemStack();
        if (stack.getType() == Material.STICK) {
            final Player player = event.getPlayer();
            if (inSuitableOperatorMode(player)) {
                if (Session.has(player)) {
                    event.setCancelled(true);
                    final Session session = Session.getOrCreate(player);
                    final boolean newState = !session.isActive();
                    session.setActive(newState);
                    if (newState) {
                        player.sendMessage(ChatColor.GREEN + "现在开始继续记录！再扔一次木棍暂停记录！");
                    } else {
                        player.sendMessage(ChatColor.RED + "现在开始暂停记录！再扔一次木棍继续记录！");
                    }
                    playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (barrierRelated(event)) {
            final Block block = event.getBlock();
            final Player player = event.getPlayer();
            if (inSuitableOperatorMode(player)) {
                if (inOperate(player)) {
                    final Session session = Session.getOrCreate(player);
                    session.remove(block.getLocation());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDropBarrier(PlayerDropItemEvent event) {
        final ItemStack stack = event.getItemDrop().getItemStack();
        if (stack.getType() == Material.BARRIER) {
            final Player player = event.getPlayer();
            if (inSuitableOperatorMode(player)) {
                if (inOperate(player)) {
                    event.setCancelled(true);
                    final UUID uuid = player.getUniqueId();
                    if (aboutToQueue.containsKey(uuid)) {
                        final Session session = Session.getOrCreate(player);
                        final boolean started = session.startPlacing();
                        if (started) {
                            player.sendMessage(ChatColor.GREEN + "操作成功！");
                            playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
                        } else {
                            player.sendMessage(ChatColor.RED + "队列中没有待处理的方块！");
                            playSound(player, Sound.ENTITY_VILLAGER_NO);
                        }
                        aboutToQueue.remove(uuid).cancel();
                    } else {
                        player.sendMessage(ChatColor.GREEN + "在 10 秒内再按一次丢弃以确认提交(提交后开始放置)！");
                        final BukkitTask task = getServer().getScheduler().runTaskLater(this, () -> {
                            aboutToQueue.remove(uuid);
                        }, 200L);
                        aboutToQueue.put(uuid, task);
                    }
                }
            }
        }
    }

    @EventHandler
    public void ad(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        player.sendMessage(ChatColor.GREEN + "FastBarrier 插件正在运行。作者: ZX夏夜之风");
    }
}
