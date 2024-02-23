package snw.fastbarrier;

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Session {
    private static final Map<UUID, Session> session = new HashMap<>();
    private final UUID uuid;
    @Getter
    private final List<Location> queuedLocation = new ArrayList<>();
    // WARN: actions saved in the following queue are not reversed.
    // what does it mean?
    // actions saved in undoQueue are going to place barrier (not air)
    private final Deque<List<CompareAndPlaceAction>> undoQueue = new ArrayDeque<>();
    private final Deque<List<CompareAndPlaceAction>> redoQueue = new ArrayDeque<>();
    @Getter
    @Setter
    private boolean active = true;
    @Setter
    private int height = Config.wallHeight();
    @Setter
    private Material blockType = Config.newBlockType();

    // return queued location count
    public int append(Location location) {
        queuedLocation.add(location);
        return queuedLocation.size();
    }

    public void remove(Location location) {
        queuedLocation.remove(location);
    }

    // return false if no location is queued
    public boolean startPlacing() {
        if (!queuedLocation.isEmpty()) {
            final List<CompareAndPlaceAction> actions = CompareAndPlaceAction.airTo(queuedLocation, blockType, height);
            BlockPlacer.queue.addAll(actions);
            undoQueue.addFirst(ImmutableList.copyOf(actions));
            if (undoQueue.size() > Config.maxUndoSteps()) {
                undoQueue.removeLast(); // drop it
            }
            redoQueue.clear();
            queuedLocation.clear();
            return true;
        } else {
            return false;
        }
    }

    public boolean undo() {
        return queueReversedActionAndSave(undoQueue, redoQueue);
    }

    public boolean redo() {
        return queueReversedActionAndSave(redoQueue, undoQueue);
    }

    private boolean queueReversedActionAndSave(Deque<List<CompareAndPlaceAction>> from, Deque<List<CompareAndPlaceAction>> to) {
        if (!from.isEmpty()) {
            final List<CompareAndPlaceAction> actions =
                    from.removeLast()
                            .stream()
                            .map(CompareAndPlaceAction::reverse)
                            .toList();
            BlockPlacer.queue.addAll(actions);
            to.add(actions);
            return true;
        } else {
            return false;
        }
    }

    public static boolean has(Player player) {
        return session.containsKey(player.getUniqueId());
    }

    public static Session getOrCreate(Player player) {
        return session.computeIfAbsent(player.getUniqueId(), Session::new);
    }

    public static void destroyAll() {
        session.clear();
    }

    public void destroy() {
        session.values().removeIf(it -> it == this);
        queuedLocation.clear();
        undoQueue.clear();
        redoQueue.clear();
    }
}
