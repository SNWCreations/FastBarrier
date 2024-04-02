package snw.fastbarrier;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

// executes on main thread
public final class BlockPlacer implements Runnable {
    public static final Queue<CompareAndPlaceAction> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void run() {
        int steps = Config.processedUnitPerTick();
        CompareAndPlaceAction action;
        while (steps-- >= 0 && (action = queue.poll()) != null) {
            action.run();
        }
    }
}
