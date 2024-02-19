package snw.fastbarrier;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public record CompareAndPlaceAction(@NonNull Location basePoint, @Nullable Material from, @NonNull Material to, int height) implements Runnable {

    public static CompareAndPlaceAction airToBarrier(Location location, int height) {
        return new CompareAndPlaceAction(location, Material.AIR, Material.BARRIER, height);
    }

    public static List<CompareAndPlaceAction> airToBarrier(Collection<Location> locations, int height) {
        return locations.stream().map(i -> airToBarrier(i, height)).toList();
    }

    public static CompareAndPlaceAction barrierToAir(Location location, int height) {
        return new CompareAndPlaceAction(location, Material.BARRIER, Material.AIR, height);
    }

    public static List<CompareAndPlaceAction> barrierToAir(Collection<Location> locations, int height) {
        return locations.stream().map(i -> barrierToAir(i, height)).toList();
    }

    public CompareAndPlaceAction reverse() {
        Preconditions.checkNotNull(from, "from must not be null");
        return new CompareAndPlaceAction(basePoint, to, from, height);
    }

    @Override
    public void run() {
        for (Block block : constructList()) {
            if (from == null || block.getType() == from) {
                block.setType(to, false);
            }
        }
    }

    private List<Block> constructList() {
        final ArrayList<Block> blocks = new ArrayList<>();
        int theHeight = height;
        Block cursor = basePoint.getBlock();
        final int maxHeight = cursor.getWorld().getMaxHeight() - 1;
        while (theHeight-- > 0) {
            if (cursor.getLocation().getBlockY() > maxHeight) { // consider max height
                break;
            }
            blocks.add(cursor);
            cursor = cursor.getRelative(BlockFace.UP);
        }
        return blocks;
    }
}
