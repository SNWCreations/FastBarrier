package snw.fastbarrier;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

// executes on main thread
public final class BlockPlacer implements Runnable {
    public static final Queue<CompareAndPlaceAction> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void run() {
        int steps = Config.processedUnitPerTick();
        CompareAndPlaceAction action = queue.poll();
        while (action != null && steps-- >= 0) {
            action.run();
            action = queue.poll();
        }
    }
}
